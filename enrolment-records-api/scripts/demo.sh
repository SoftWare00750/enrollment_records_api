#!/usr/bin/env bash
# Walks through the main flows against a running API started with the "demo" profile.
#   docker compose up -d
#   mvn spring-boot:run -Dspring-boot.run.profiles=demo
#   ./scripts/demo.sh            (BASE=http://localhost:8080 by default)
#
# Seed data: academic year 1 = 2026/2027 (open). Grade 1 = PRI1 (only 2 seats), grade 2 = PRI4.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
ACTOR="registrar.jane"

post_json() { curl -sS -X POST "$BASE$1" -H 'Content-Type: application/json' -H "X-Actor: $ACTOR" -d "$2"; }
post_empty() { curl -sS -X POST "$BASE$1" -H "X-Actor: $ACTOR"; }
get() { curl -sS "$BASE$1"; }
id_of() { grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }
step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }
show() { printf '%s\n' "$1" | python3 -m json.tool 2>/dev/null || printf '%s\n' "$1"; }

# application_json FIRST LAST DOB EMAIL [yes|no = include immunisation record] [name on birth certificate]
application_json() {
  local imm=',{"type":"IMMUNISATION_RECORD"}'
  [ "${5:-yes}" = "no" ] && imm=''
  local cert_name="${6:-$1 $2}"
  cat <<JSON
{"applicant":{"firstName":"$1","lastName":"$2","dateOfBirth":"$3"},
 "guardian":{"fullName":"Parent of $1","email":"$4","phone":"+2348012345678"},
 "academicYearId":1,"gradeLevelId":1,
 "documents":[{"type":"BIRTH_CERTIFICATE","nameOnDocument":"$cert_name","dateOnDocument":"$3"},
              {"type":"PASSPORT_PHOTO"}$imm]}
JSON
}

decision() { printf '{"decision":"%s","reason":"%s"}' "$1" "$2"; }

step "Seats available"
show "$(get /api/offerings)"

step "1. Ada: a clean application -> eligible -> admitted"
ADA=$(post_json /api/applications "$(application_json Ada Okafor 2020-03-10 ada.$$@example.com)" | id_of)
show "$(post_empty /api/applications/$ADA/eligibility-check)"
show "$(post_json /api/applications/$ADA/decision "$(decision ADMITTED 'Meets all criteria')")"

step "2. Bola: name on birth certificate does not match -> advisory flag for staff"
BOLA=$(post_json /api/applications "$(application_json Bola Adeyemi 2020-05-02 bola.$$@example.com yes 'Tunde Bakare')" | id_of)
show "$(get /api/applications/$BOLA/document-review)"
post_empty /api/applications/$BOLA/eligibility-check > /dev/null
show "$(post_json /api/applications/$BOLA/decision "$(decision ADMITTED 'Staff confirmed the certificate was a clerical error')")"

step "3. Chidi: class is now full -> admission refused (409) -> waitlisted"
CHIDI=$(post_json /api/applications "$(application_json Chidi Eze 2020-08-19 chidi.$$@example.com)" | id_of)
post_empty /api/applications/$CHIDI/eligibility-check > /dev/null
show "$(post_json /api/applications/$CHIDI/decision "$(decision ADMITTED 'Try to admit')")"
show "$(post_json /api/applications/$CHIDI/decision "$(decision WAITLISTED 'Class full')")"

step "4. Dayo: too old for Primary 1 -> ineligible, cannot be admitted, rejected"
DAYO=$(post_json /api/applications "$(application_json Dayo Bello 2012-01-01 dayo.$$@example.com)" | id_of)
show "$(post_empty /api/applications/$DAYO/eligibility-check)"
show "$(post_json /api/applications/$DAYO/decision "$(decision ADMITTED 'Try to admit')")"
show "$(post_json /api/applications/$DAYO/decision "$(decision REJECTED 'Outside the Primary 1 age range')")"

step "5. Emeka: missing immunisation record -> ineligible -> document added -> eligible"
EMEKA=$(post_json /api/applications "$(application_json Emeka Obi 2020-01-01 emeka.$$@example.com no)" | id_of)
show "$(post_empty /api/applications/$EMEKA/eligibility-check)"
post_json /api/applications/$EMEKA/documents '{"type":"IMMUNISATION_RECORD"}' > /dev/null
show "$(post_empty /api/applications/$EMEKA/eligibility-check)"

step "6. Validation errors come back as one consistent shape (400)"
show "$(post_json /api/applications '{"applicant":{"firstName":"","dateOfBirth":"2999-01-01"},"guardian":{"email":"nope"}}')"

step "Audit history for Ada"
show "$(get /api/applications/$ADA/audit)"

step "All admitted / waitlisted applications"
show "$(get '/api/applications?status=ADMITTED')"
show "$(get '/api/applications?status=WAITLISTED')"

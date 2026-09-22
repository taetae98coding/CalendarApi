# CalendarApi

공휴일과 음력 정보를 **정적 JSON API** 로 제공하는 프로젝트입니다.

여러 공개 API 에서 데이터를 취합해 파일로 관리하고, GitHub Actions 가 주기적으로 갱신한 뒤 GitHub Pages 로 배포합니다.
클라이언트는 인증키 없이 JSON 파일 하나만 받으면 됩니다.

👉 **https://taetae98coding.github.io/CalendarApi/**

## 데이터 출처

| 데이터 | 출처 | 제공 범위 |
| --- | --- | --- |
| 한국 공휴일 | [한국천문연구원 특일 정보](https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15012690) | 2004년 ~ 고시된 연도 |
| 한국 공휴일 (보완) | [Nager.Date](https://date.nager.at) | 1975년 ~ 2076년 |
| 미국 공휴일 | [Nager.Date](https://date.nager.at) | 1975년 ~ 2076년 |
| 음력 | [한국천문연구원 음양력 정보](https://www.data.go.kr/data/15012679/openapi.do) | 1391-02-05 ~ 2050-12-31 |

한국 공휴일은 한국천문연구원 특일 정보를 우선으로 사용합니다.
특일 정보에 공휴일이 없는 연도(2004년 이전, 아직 고시되지 않은 미래 연도)는 Nager.Date 로 보완해
**1998년부터 올해 + 3년까지** 끊김 없이 제공합니다.

## API

모든 경로의 베이스는 `https://taetae98coding.github.io/CalendarApi/` 입니다.
`{country}` 는 `kr`, `us` 이고 `{month}` 는 `01` ~ `12` 입니다.

### 공휴일

```
holiday/{country}/{year}.json
holiday/{country}/{year}-{month}.json
```

```json
[
    {
        "name": "설날",
        "isHoliday": true,
        "start": "2025-01-28",
        "endInclusive": "2025-01-30"
    }
]
```

연속된 같은 이름의 날짜는 하나의 기간으로 합쳐집니다.
`isHoliday` 가 `false` 인 항목은 절기·잡절·기념일처럼 쉬지 않는 날입니다.

### 음력

```
lunar/{year}.json
lunar/{year}-{month}.json
```

```json
[
    {
        "solar": "2025-01-01",
        "year": 2024,
        "month": 12,
        "day": 2,
        "isLeapMonth": false
    }
]
```

음력은 윤달이 있고 달의 크기가 29·30일로 달라 날짜 타입으로 표현할 수 없으므로 연·월·일을 분리해 담습니다.

### 통합

공휴일과 음력을 한 번의 요청으로 받습니다.

```
calendar/{country}/{year}.json
calendar/{country}/{year}-{month}.json
```

```json
{
    "country": "kr",
    "period": "2025-01",
    "holidays": [],
    "lunar": []
}
```

### 메타

```
meta.json
```

갱신 시각, 제공 범위, 아직 생성되지 않은 음력 연도를 확인할 수 있습니다.

## 클라이언트 예시 (Kotlin / Ktor)

```kotlin
@Serializable
data class CalendarResponse(
    val country: String,
    val period: String,
    val holidays: List<Holiday>,
    val lunar: List<LunarDate>,
)

suspend fun HttpClient.getCalendar(country: String, yearMonth: String): CalendarResponse {
    return get("https://taetae98coding.github.io/CalendarApi/calendar/$country/$yearMonth.json").body()
}
```

GitHub Pages 는 `Access-Control-Allow-Origin: *` 를 내려주므로 웹에서도 그대로 호출할 수 있습니다.

## 갱신 방식

`.github/workflows/update-calendar.yml` 이 매일 00:00 UTC 에 실행됩니다.

- 이미 지난 달의 공휴일 응답은 `cache/` 에 저장해 다시 호출하지 않습니다.
- 음력은 한 번 확정되면 바뀌지 않으므로 이미 만들어진 월 파일을 그대로 재사용합니다.
- 한 번의 실행에서 새로 호출할 음력 월 수를 `LUNAR_FETCH_BUDGET`(기본 3000) 으로 제한합니다.
  공공데이터포털 개발 계정의 일일 트래픽 한도 안에서, 여러 번의 실행에 걸쳐 1391 ~ 2050년 전체를 채웁니다.
  현재 연도에 가까운 연도부터 채우므로 실제로 많이 쓰이는 구간이 먼저 완성됩니다.
- 조회에 실패해 결과가 비면 기존 파일을 덮어쓰지 않습니다.

수집은 코루틴으로 병렬 처리합니다.

- 특일 정보와 음양력 정보는 서로 다른 서비스라 트래픽 한도가 따로 잡히므로 공휴일과 음력을 동시에 수집합니다.
- 공휴일은 국가 × 연도 × 월 × API 를 모두 동시에 호출합니다.
- 음력은 `Semaphore` 로 4개 연도씩 처리하고, 한 연도 안에서 12개 월을 동시에 호출합니다.
  `Semaphore` 가 FIFO 라 "가까운 연도 먼저" 순서가 유지됩니다.
- 외부 API 한 곳으로 동시에 나가는 요청 수는 `MAX_CONCURRENCY`(기본 8) 로 제한하고,
  서버 오류·타임아웃은 Ktor `HttpRequestRetry` 가 지수 백오프로 3회까지 재시도합니다.

## 로컬 실행

공공데이터포털에서 [특일 정보](https://www.data.go.kr/tcs/dss/selectApiDataDetailView.do?publicDataPk=15012690)와
[음양력 정보](https://www.data.go.kr/data/15012679/openapi.do) 활용 신청 후 받은 **일반 인증키(Decoding)** 가 필요합니다.

```bash
cp secrets.properties.example secrets.properties
# secrets.properties 를 열어 SERVICE_KEY 값을 채웁니다. 이 파일은 .gitignore 대상입니다.

./gradlew updateCalendar
```

환경 변수로 넘겨도 됩니다. 환경 변수가 `secrets.properties` 보다 우선합니다.

```bash
SERVICE_KEY='발급받은 인증키' ./gradlew updateCalendar
```

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SERVICE_KEY` | (필수) | 공공데이터포털 인증키 |
| `START_YEAR` | `1998` | 공휴일 시작 연도 |
| `END_INCLUSIVE_YEAR` | `올해 + 3` | 공휴일 종료 연도 |
| `LUNAR_START_YEAR` | `1391` | 음력 시작 연도 |
| `LUNAR_END_INCLUSIVE_YEAR` | `2050` | 음력 종료 연도 |
| `LUNAR_FETCH_BUDGET` | `3000` | 한 번의 실행에서 새로 호출할 음력 월 수 |
| `FETCH_ENFORCE` | `false` | 캐시를 무시하고 모두 다시 호출 |
| `MAX_CONCURRENCY` | `8` | 외부 API 한 곳으로 나가는 동시 요청 수 |

테스트는 인증키 없이 실행할 수 있습니다.

```bash
./gradlew test
```

## 저장소 설정

1. `Settings > Secrets and variables > Actions > New repository secret` 에 `SERVICE_KEY` 를 등록합니다.
   (`secrets.properties` 에 넣는 값과 같습니다.)
2. `Settings > Pages` 에서 Source 를 `Deploy from a branch`, 브랜치를 `main`, 폴더를 `/docs` 로 설정합니다.
3. `Actions > Update Calendar > Run workflow` 로 첫 데이터를 생성합니다.

## 라이선스

[MIT](LICENSE)

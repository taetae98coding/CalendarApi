package io.github.taetae98coding.calendar.publish

import io.github.taetae98coding.calendar.domain.Country

/**
 * 소비처에 보여 줄 출처 설명. `meta.json` 의 `sources` 가 된다.
 *
 * 자료가 어디서 왔는지는 배포 계층이 모른다. 저장소를 조립하는 쪽이 어느 원천을 붙였는지 알므로 그쪽이 채운다.
 */
fun interface SourceCatalog {
    fun sources(api: DocApi, country: Country): List<String>
}

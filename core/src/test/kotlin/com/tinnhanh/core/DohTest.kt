package com.tinnhanh.core
import kotlin.test.Test
import kotlin.test.assertEquals

class DohTest {
    @Test fun parseAnswerLayIpv4() {
        val json = """{"Status":0,"Answer":[{"name":"a.xxx","type":1,"TTL":300,"data":"104.21.5.6"},{"name":"a.xxx","type":1,"TTL":300,"data":"172.67.1.2"}]}"""
        assertEquals(listOf("104.21.5.6", "172.67.1.2"), parseDohAnswer(json))
    }

    @Test fun boQuaTypeKhongPhaiA() {
        // type 5 = CNAME, bỏ qua; chỉ lấy type 1 (A)
        val json = """{"Answer":[{"type":5,"data":"x.cdn.net"},{"type":1,"data":"1.2.3.4"}]}"""
        assertEquals(listOf("1.2.3.4"), parseDohAnswer(json))
    }

    @Test fun khongCoAnswerTraRong() {
        assertEquals(emptyList(), parseDohAnswer("""{"Status":2}"""))
    }
}

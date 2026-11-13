package net.schedion.app.data.remote.dto

import net.schedion.app.data.remote.SchedionJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Проверяет, что типовые ответы API корректно десериализуются в DTO. */
class DtoDeserializationTest {

    @Test
    fun scheduleWeek_isParsedWithLessonsAndCancellations() {
        val week = SchedionJson.decodeFromString<ScheduleWeekDto>(SCHEDULE_WEEK_JSON)

        assertEquals("2026-11-09", week.weekStart)
        assertEquals("2026-11-12", week.nextLessonDate)
        assertEquals(1, week.lessons.size)

        val lesson = week.lessons.single()
        assertEquals(LessonKind.LECTURE, lesson.kind)
        assertEquals("Математический анализ", lesson.discipline)
        assertNull(lesson.room)
        assertEquals(listOf("Иванов И.И."), lesson.teachers.map { it.shortName })

        val cancellation = week.cancellations.single()
        assertEquals(listOf(3), cancellation.pairs)
        assertNull(cancellation.course)
        assertEquals(1, week.filters.groups.size)
    }

    @Test
    fun weekList_isParsedWithColors() {
        val weeks = SchedionJson.decodeFromString<WeekListDto>(WEEK_LIST_JSON)

        assertEquals("2026/2027 уч.год, 1 семестр", weeks.semester.title)
        assertEquals(listOf(WeekColor.BLACK, WeekColor.RED), weeks.items.map { it.color })
    }

    @Test
    fun newsFeed_isParsedWithPaging() {
        val feed = SchedionJson.decodeFromString<NewsFeedDto>(NEWS_FEED_JSON)

        assertEquals(42, feed.total)
        assertEquals(0, feed.offset)
        assertEquals(1, feed.items.size)
        assertNull(feed.items.single().coverUrl)
    }

    @Test
    fun homeContent_isParsedWithNullableBanner() {
        val home = SchedionJson.decodeFromString<HomeContentDto>(HOME_CONTENT_JSON)

        assertNull(home.banner)
        assertEquals(1, home.events.size)
        assertEquals(true, home.events.single().forApplicants)
        assertEquals("Расписание на неделю", home.documents.single().title)
    }

    private companion object {
        const val SCHEDULE_WEEK_JSON = """
        {
          "weekStart": "2026-11-09",
          "weekEnd": "2026-11-15",
          "lessons": [
            {
              "id": 12,
              "date": "2026-11-09",
              "pair": 1,
              "kind": "lecture",
              "disciplineId": 4,
              "discipline": "Математический анализ",
              "room": null,
              "subgroup": null,
              "groupId": 3,
              "groupName": "2ИСиП_ИС1",
              "course": 2,
              "associationName": null,
              "associationGroupIds": [],
              "teachers": [{"id": 7, "shortName": "Иванов И.И."}],
              "cancellationReason": null
            }
          ],
          "cancellations": [
            {
              "startDate": "2026-11-10",
              "endDate": "2026-11-10",
              "pairs": [3],
              "reason": "Болезнь преподавателя",
              "scope": "group",
              "groupId": 3,
              "course": null
            }
          ],
          "filters": {
            "groups": [{"id": 3, "name": "2ИСиП_ИС1", "description": null, "course": 2}],
            "teachers": [{"id": 7, "fullName": "Иванов Иван Иванович", "shortName": "Иванов И.И."}],
            "courses": [2]
          },
          "nextLessonDate": "2026-11-12"
        }
        """

        const val WEEK_LIST_JSON = """
        {
          "semester": {"start": "2026-09-01", "end": "2026-12-27", "title": "2026/2027 уч.год, 1 семестр"},
          "currentWeekStart": "2026-11-09",
          "defaultWeekStart": "2026-11-09",
          "items": [
            {"start": "2026-11-02", "end": "2026-11-08", "number": 10, "color": "black"},
            {"start": "2026-11-09", "end": "2026-11-15", "number": 11, "color": "red"}
          ]
        }
        """

        const val NEWS_FEED_JSON = """
        {
          "items": [
            {
              "slug": "den-otkrytyh-dverey",
              "title": "День открытых дверей",
              "summary": "Приглашаем абитуриентов.",
              "date": "2026-11-10",
              "publishedAt": "2026-11-10T09:30:00Z",
              "coverUrl": null
            }
          ],
          "total": 42,
          "offset": 0,
          "count": 1
        }
        """

        const val HOME_CONTENT_JSON = """
        {
          "banner": null,
          "weekHighlight": null,
          "events": [
            {
              "title": "День открытых дверей",
              "date": "2026-11-14",
              "time": "10:00:00",
              "place": "Актовый зал",
              "linkUrl": null,
              "forApplicants": true
            }
          ],
          "nextApplicantEvent": null,
          "documents": [
            {
              "slug": "raspisanie-na-nedelyu",
              "title": "Расписание на неделю",
              "description": null,
              "audience": ["students", "teachers"],
              "fileUrl": "https://schedion.ru/files/schedule.pdf",
              "fileName": "schedule.pdf",
              "format": "pdf",
              "size": 10240,
              "updatedAt": "2026-11-08"
            }
          ]
        }
        """
    }
}

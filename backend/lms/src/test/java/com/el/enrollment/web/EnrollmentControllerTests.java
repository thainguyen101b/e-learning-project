package com.el.enrollment.web;

import com.el.TestFactory;
import com.el.common.config.CustomAuthenticationEntryPoint;
import com.el.common.config.SecurityConfig;
import com.el.common.config.jackson.JacksonCustomizations;
import com.el.common.exception.InputInvalidException;
import com.el.common.exception.ResourceNotFoundException;
import com.el.course.domain.QuestionType;
import com.el.enrollment.application.dto.ChangeCourseResponse;
import com.el.enrollment.application.dto.CourseEnrollmentDTO;
import com.el.enrollment.web.dto.QuestionSubmitDTO;
import com.el.enrollment.web.dto.QuizSubmitDTO;
import com.el.enrollment.application.impl.CourseEnrollmentServiceImpl;
import com.el.enrollment.domain.Enrollment;
import com.el.enrollment.domain.EnrollmentRepository;
import com.el.enrollment.domain.LessonProgress;
import com.el.enrollment.web.dto.LessonMarkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(EnrollmentController.class)
@Import({SecurityConfig.class, JacksonCustomizations.class, CustomAuthenticationEntryPoint.class})
class EnrollmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseEnrollmentServiceImpl courseEnrollmentService;

    @MockBean
    private EnrollmentRepository enrollmentRepository;

    @Test
    void getAllEnrollments_ValidRequest_ReturnsAllEnrollments() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);

        Enrollment enrollment = new Enrollment("user", 1L, "teacher", Set.of(
                new LessonProgress("Course Lesson 1", 1L, 1),
                new LessonProgress("Course Lesson 2", 2L, 2)),
                Set.of(1L, 2L));
        CourseEnrollmentDTO enrollmentDTO = new CourseEnrollmentDTO(
                enrollment.getId(),
                enrollment.getStudent(),
                enrollment.getCourseId(),
                "demo title",
                "https://example.com",
                TestFactory.teacher,
                LocalDateTime.now(),
                false
        );
        when(courseEnrollmentService.findAllCourseEnrollments(pageable)).thenReturn(List.of(enrollmentDTO));

        mockMvc.perform(get("/enrollments")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void markLessonAsCompleted_ValidRequest_MarksLessonAsCompleted() throws Exception {
        doNothing().when(courseEnrollmentService).markLessonAsCompleted(any(), any(), any());

        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.COMPLETED, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isOk());
    }

    @Test
    void markLessonAsCompleted_InvalidRequest_ReturnsBadRequest() throws Exception {
        var lessonMarkJson = """
                {
                    "mark": "invalid",
                    "courseId": 1,
                    "lessonId": 1
                }
                """;

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(lessonMarkJson)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void markLessonAsCompleted_NotAuthorized_ReturnsForbidden() throws Exception {
        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.COMPLETED, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void markLessonAsCompleted_NotFound_ReturnsNotFound() throws Exception {
        doThrow(ResourceNotFoundException.class).when(courseEnrollmentService).markLessonAsCompleted(any(), any(), any());
        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.COMPLETED, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isNotFound());
    }

    @Test
    void markLessonAsIncomplete_ValidRequest_MarksLessonAsIncomplete() throws Exception {
        doNothing().when(courseEnrollmentService).markLessonAsIncomplete(any(), any(), any());
        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.INCOMPLETE, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isOk());
    }

    @Test
    void markLessonAsIncomplete_NotAuthorized_ReturnsForbidden() throws Exception {
        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.INCOMPLETE, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void markLessonAsIncomplete_NotFound_ReturnsNotFound() throws Exception {
        doThrow(ResourceNotFoundException.class).when(courseEnrollmentService).markLessonAsIncomplete(any(), any(), any());
        var lessonMark = new LessonMarkRequest(LessonMarkRequest.MarkType.INCOMPLETE, 1L, 1L);

        mockMvc.perform(put("/enrollments/{enrollmentId}/mark-lesson", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(lessonMark))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isNotFound());
    }

    @Test
    void quizSubmission_ValidRequest_SubmitsQuiz() throws Exception {
//        when(courseEnrollmentService.submitQuiz(any(), any())).thenReturn()

        QuizSubmitDTO quizSubmitDTO = TestFactory.createQuizSubmitDTO();

        mockMvc.perform(post("/enrollments/{enrollmentId}/submit-quiz", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(quizSubmitDTO))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isOk());
    }

    @Test
    void quizSubmission_InvalidRequest_BadRequest() throws Exception {
//        doNothing().when(courseEnrollmentService).submitQuiz(any(), any());

        QuizSubmitDTO quizSubmitDTO = new QuizSubmitDTO(
                1L,
                Set.of(new QuestionSubmitDTO(QuestionType.SINGLE_CHOICE, 1L, Set.of(1L, 2L), null, null),
                        new QuestionSubmitDTO(QuestionType.TRUE_FALSE, 2L, null, true, null)
                ));

        mockMvc.perform(post("/enrollments/{enrollmentId}/submit-quiz", 1L)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(quizSubmitDTO))
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user")))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void changeCourse_ValidRequest_ChangesCourse() throws Exception {
        ChangeCourseResponse response = ChangeCourseResponse.basicChange();
        when(courseEnrollmentService.changeCourse(anyLong(), anyLong())).thenReturn(response);

        mockMvc.perform(put("/enrollments/{enrollmentId}/change-course", 1L)
                        .param("courseId", "2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.type").value("BASIC_CHANGE"))
                .andExpect(jsonPath("$.priceAdditional").doesNotExist())
                .andExpect(jsonPath("$.orderId").doesNotExist());
    }

    @Test
    void changeCourse_InvalidCourseId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/enrollments/{enrollmentId}/change-course", 1L)
                        .param("courseId", "invalid")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeCourse_NotAuthorized_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/enrollments/{enrollmentId}/change-course", 1L)
                        .param("courseId", "2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeCourse_AppServiceThrowThenBadRequest() throws Exception {
        when(courseEnrollmentService.changeCourse(anyLong(), anyLong())).thenThrow(InputInvalidException.class);

        mockMvc.perform(put("/enrollments/{enrollmentId}/change-course", 1L)
                        .param("courseId", "2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isBadRequest());
    }

}

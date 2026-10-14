package com.tianji.aigc.tools;

import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.tianji.aigc.constants.Constant.REQUEST_ID;

@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseClient courseClient;

    private final static String FIELD = "courseinfo_";

    private final StringRedisTemplate  stringRedisTemplate;

    /**
     * 根据课程id查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        CourseInfo courseInfo = courseId == null ? null : CourseInfo.of(this.courseClient.baseInfo(courseId, true));
        String requestId = (String) toolContext.getContext().get(Constant.REQUEST_ID);
        stringRedisTemplate.opsForHash().put(requestId,FIELD+courseInfo.getId(), courseInfo);
        return courseInfo;
    }
}
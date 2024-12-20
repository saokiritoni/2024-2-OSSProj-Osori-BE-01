package dongguk.osori.domain.goal.service;

import dongguk.osori.domain.goal.dto.GoalCommentResponseDto;
import dongguk.osori.domain.goal.dto.GoalDetailResponseDto;
import dongguk.osori.domain.goal.dto.GoalCommentRequestDto;
import dongguk.osori.domain.goal.entity.Goal;
import dongguk.osori.domain.goal.entity.GoalComment;
import dongguk.osori.domain.goal.repository.GoalCommentRepository;
import dongguk.osori.domain.goal.repository.GoalRepository;
import dongguk.osori.domain.quest.entity.MissionType;
import dongguk.osori.domain.quest.service.QuestService;
import dongguk.osori.domain.user.entity.User;
import dongguk.osori.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalCommentService {

    private final GoalCommentRepository goalCommentRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final QuestService questService;

    // 댓글 추가
    @Transactional
    public GoalCommentResponseDto addComment(Long goalId, Long userId, GoalCommentRequestDto goalRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        GoalComment comment = GoalComment.builder()
                .content(goalRequestDto.getContent())
                .emoji(goalRequestDto.getEmoji())
                .goal(goal)
                .user(user)
                .build();

        goalCommentRepository.save(comment);

        questService.updateMissionStatus(userId, MissionType.COMMENTED_ON_FRIEND_GOAL);

        return new GoalCommentResponseDto(
                comment.getCommentId(),
                user.getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getEmoji(),
                true // 댓글 작성자는 로그인된 사용자이므로 항상 true
        );
    }

    // 댓글 조회
    @Transactional
    public Optional<GoalDetailResponseDto> getGoalDetailsWithComments(Long goalId, Long userId) {
        User loggedInUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return goalRepository.findById(goalId)
                .map(goal -> {
                    List<GoalCommentResponseDto> comments = goal.getComments().stream()
                            .map(comment -> new GoalCommentResponseDto(
                                    comment.getCommentId(),
                                    comment.getUser().getNickname(),
                                    comment.getContent(),
                                    comment.getCreatedAt(),
                                    comment.getEmoji(),
                                    comment.getUser().equals(loggedInUser) // 댓글 작성자와 로그인된 사용자 비교
                            ))
                            .collect(Collectors.toList());

                    return new GoalDetailResponseDto(
                            goal.getGoalId(),
                            goal.getContent(),
                            goal.getUser().getNickname(),
                            goal.getCreatedAt(),
                            goal.isCompleted(),
                            goal.getUser().equals(loggedInUser), // 목표 작성자와 로그인된 사용자 비교
                            comments
                    );
                });
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        GoalComment comment = goalCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized user");
        }

        goalCommentRepository.deleteById(commentId);
    }
}


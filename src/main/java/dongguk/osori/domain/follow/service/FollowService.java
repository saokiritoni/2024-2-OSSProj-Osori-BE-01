package dongguk.osori.domain.follow.service;

import dongguk.osori.domain.follow.FollowRepository;
import dongguk.osori.domain.follow.dto.FollowDto;
import dongguk.osori.domain.follow.entity.Follow;
import dongguk.osori.domain.user.UserRepository;
import dongguk.osori.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    // TODO: 나를 팔로잉 하는 목록 만들기

    // 로그인된 사용자의 ID 가져오기
    private Long getLoggedInUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 내 팔로잉 목록
    public List<FollowDto> getMyFollowings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return followRepository.findByFollower(user).stream()
                .map(f -> new FollowDto(
                        f.getFollowing().getUserId(),
                        f.getFollowing().getNickname(),
                        f.getFollowing().getEmail()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void followUser(Long userId, Long followingUserId) {
        User loggedInUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        User followingUser = userRepository.findById(followingUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + followingUserId));

        // 이미 팔로우 관계가 존재하는지 확인
        boolean alreadyFollowing = followRepository.findByFollowerAndFollowing(loggedInUser, followingUser).isPresent();
        if (alreadyFollowing) {
            throw new IllegalArgumentException("You are already following this user.");
        }

        Follow follow = new Follow(loggedInUser, followingUser);
        followRepository.save(follow);
    }

    // 언팔로우
    @Transactional
    public void unfollowUser(Long userId, Long followingId) {
        // 현재 로그인된 사용자 가져오기
        User follower = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("Following not found"));

        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

    // 팔로워 끊기
    @Transactional
    public void blockFollower(Long userId, Long followerId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("Follower not found"));
        User following = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

}

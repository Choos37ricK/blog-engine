package project.controllers.rest;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.controllers.exceptions.UnauthorizedException;
import project.dto.StatisticsDto;
import project.models.GlobalSetting;
import project.models.enums.GlobalSettingsEnum;
import project.services.AuthService;
import project.services.GlobalSettingsService;
import project.services.PostService;
import project.services.PostVoteService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/statistics/")
@AllArgsConstructor
public class StatisticsController {

    private final AuthService authService;

    private final PostService postService;

    private final PostVoteService postVoteService;

    private final GlobalSettingsService globalSettingsService;

    @GetMapping("my")
    public ResponseEntity<?> my() {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        Integer authorId = authService.getUserIdBySession();
        Integer myPostsTotalCount = postService.countPostsByAuthorId(authorId);
        Integer myTotalLikeCount = postService.countTotalVoteCountByAuhtorIdAndValue(authorId, 1);
        Integer myTotalDislikeCount = postService.countTotalVoteCountByAuhtorIdAndValue(authorId, -1);
        Integer myTotalViewCount = postService.countViewCountByAuthorId(authorId);
        LocalDateTime myFirstPublicationDate = postService.findByAuhtorIdFirstPublicationDate(authorId);
        String myFirstPublication = myFirstPublicationDate != null ?
                myFirstPublicationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) : "";
        return ResponseEntity.ok(new StatisticsDto(
                myPostsTotalCount,
                myTotalLikeCount,
                myTotalDislikeCount,
                myTotalViewCount != null ? myTotalViewCount : 0,
                myFirstPublication
        ));
    }

    @GetMapping("all")
    public ResponseEntity<?> all() {

        GlobalSetting statisticsIsPublic = globalSettingsService.getGlobalSettingByCode(GlobalSettingsEnum.STATISTICS_IS_PUBLIC);

        if (!authService.checkAuthorization() && statisticsIsPublic != null && statisticsIsPublic.getValue().equals("NO")) {
            throw new UnauthorizedException();
        }

        Integer postsTotalCount = postService.countPosts();
        Integer totalLikesCount = postVoteService.countVotesByValue(1);
        Integer totalDislikesCount = postVoteService.countVotesByValue(-1);
        Integer totalViewsCount = postService.countViewCount();
        LocalDateTime firstPublicationDate = postService.findFirstPublicationDate();
        String firstPublication = firstPublicationDate != null ?
                        firstPublicationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")) : "";
        return ResponseEntity.ok(new StatisticsDto(
                postsTotalCount,
                totalLikesCount,
                totalDislikesCount,
                totalViewsCount != null ? totalViewsCount : 0,
                firstPublication
        ));
    }
}

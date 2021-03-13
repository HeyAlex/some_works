package com.alex.miet.mobile.api;

import java.util.List;

import com.alex.miet.mobile.model.news.ArticleResponse;
import com.alex.miet.mobile.model.schedule.SemesterData;
import io.reactivex.Single;
import retrofit2.http.Query;

/**
 * Class used to get instance of {@link UniversityService}
 */
public class UniversityApiFactory {
    private static final UniversityService instance = new UniversityService() {

        private UniversityService universityService
                = UniversityServiceGenerator.createScheduleService(UniversityService.class);

        @Override
        public Single<ArticleResponse> getNews() {
            return universityService.getNews();
        }

        @Override
        public Single<List<String>> getGroupNames() {
            return universityService.getGroupNames();
        }

        @Override
        public Single<SemesterData> getScheduleResponse(@Query("group") String groupName) {
            return universityService.getScheduleResponse(groupName);
        }
    };

    public static UniversityService getUniversityApi() {
        return instance;
    }
}
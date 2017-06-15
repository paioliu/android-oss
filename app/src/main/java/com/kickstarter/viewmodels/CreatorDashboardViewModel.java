package com.kickstarter.viewmodels;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.kickstarter.libs.ActivityViewModel;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.RefTag;
import com.kickstarter.libs.rx.transformers.Transformers;
import com.kickstarter.libs.utils.ListUtils;
import com.kickstarter.libs.utils.NumberUtils;
import com.kickstarter.libs.utils.ProjectUtils;
import com.kickstarter.models.Project;
import com.kickstarter.services.ApiClientType;
import com.kickstarter.services.apiresponses.ProjectsEnvelope;
import com.kickstarter.ui.activities.CreatorDashboardActivity;

import java.util.List;

import rx.Notification;
import rx.Observable;
import rx.subjects.PublishSubject;

public interface CreatorDashboardViewModel {
  interface Inputs {
    void projectViewClicked();
  }

  interface Outputs {

    Observable<Project> latestProject();
    Observable<String> projectBackersCountText();
    Observable<String> projectNameTextViewText();
    Observable<Pair<Project, RefTag>> startProjectActivity();
    Observable<String> timeRemaining();
  }

  final class ViewModel extends ActivityViewModel<CreatorDashboardActivity> implements Inputs, Outputs {
    private final ApiClientType client;

    public ViewModel(final @NonNull Environment environment) {
      super(environment);
      this.client = environment.apiClient();

      final Observable<Notification<ProjectsEnvelope>> projectsNotification =
        this.client.fetchProjects(true).materialize().share();

      final Observable<ProjectsEnvelope> projectsEnvelope = projectsNotification
        .compose(Transformers.values());

      final Observable<List<Project>> projects = projectsEnvelope
        .map(ProjectsEnvelope::projects);

      final Observable<Project> latestProject = projects
        .map(ListUtils::first);

      this.latestProject = latestProject;

      this.projectBackersCountText = latestProject
        .map(Project::backersCount)
        .map(NumberUtils::format);

      this.projectNameTextViewText = latestProject
        .map(Project::name);

      this.startProjectActivity = latestProject
        .compose(Transformers.takeWhen(this.projectViewClicked))
        .map(p -> Pair.create(p, RefTag.dashboard()));

      this.timeRemaining = latestProject
        .map(ProjectUtils::deadlineCountdownValue)
        .map(NumberUtils::format);
    }

    private final PublishSubject<Void> projectViewClicked = PublishSubject.create();

    private final Observable<Project> latestProject;
    private final Observable<String> projectBackersCountText;
    private final Observable<String> projectNameTextViewText;
    private final Observable<Pair<Project, RefTag>> startProjectActivity;
    private final Observable<String> timeRemaining;

    public final Inputs inputs = this;
    public final Outputs outputs = this;

    @Override
    public void projectViewClicked() {
      this.projectViewClicked.onNext(null);
    }

    @Override public @NonNull Observable<Project> latestProject() {
      return this.latestProject;
    }
    @Override public @NonNull Observable<String> projectBackersCountText() {
      return this.projectBackersCountText;
    }
    @Override public @NonNull Observable<String> projectNameTextViewText() {
      return this.projectNameTextViewText;
    }
    @Override public @NonNull Observable<Pair<Project, RefTag>> startProjectActivity() {
      return this.startProjectActivity;
    }
    @Override public @NonNull Observable<String> timeRemaining() {
      return this.timeRemaining;
    }
  }
}

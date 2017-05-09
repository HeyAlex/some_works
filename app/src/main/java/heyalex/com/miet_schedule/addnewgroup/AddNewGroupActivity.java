package heyalex.com.miet_schedule.addnewgroup;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.Set;

import javax.inject.Inject;

import heyalex.com.miet_schedule.R;
import heyalex.com.miet_schedule.ScheduleApp;
import heyalex.com.miet_schedule.navdrawer.DaggerNavDrawerComponent;

/**
 * Created by mac on 28.04.17.
 */

public class AddNewGroupActivity extends AppCompatActivity implements AddNewGroupView{
    @Inject
    AddNewGroupPresenter presenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navdrawer_main);
        // inject navigation presenter
        DaggerAddNewGroupComponent.builder()
                .applicationComponent(ScheduleApp.get(this).getApplicationComponent())
                .build()
                .inject(this);
        presenter.onViewAttached(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onViewDetached();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void showAvailibleGroups(Set<String> groups) {

    }

    @Override
    public void showErrorView() {

    }
}
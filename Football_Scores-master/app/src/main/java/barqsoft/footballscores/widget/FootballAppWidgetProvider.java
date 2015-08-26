package barqsoft.footballscores.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresAdapter;
import barqsoft.footballscores.ScoresDBHelper;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.service.FetchService;

public class FootballAppWidgetProvider extends AppWidgetProvider {

	public static final String IS_WIDGET_ACTIVE = "prefIsWidgetActive";
	public static String ACTION_START_APP = "actionStartApp";
	private static final int MATCH_LENGTH = 45 + 15 + 45 + 5;

	private static class Score {

		String homeName;
		String awayName;
		String time;
		String score;
		int homeCrest;
		int awayCrest;

		public Score(String homeName, String awayName, String time, String score, int homeCrest, int awayCrest) {
			this.homeName = homeName;
			this.awayName = awayName;
			this.time = time;
			this.score = score;
			this.homeCrest = homeCrest;
			this.awayCrest = awayCrest;
		}
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		setIsWidgetActive(context, true);

		// fetch some new data
		FootballContentObserver.register(context);
		context.startService(new Intent(context, FetchService.class));
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		setIsWidgetActive(context, false);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		FootballContentObserver.register(context);
		context.startService(new Intent(context, FetchService.class));
	}

	private void setIsWidgetActive(Context context, boolean isActive) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(IS_WIDGET_ACTIVE, isActive).commit();
	}

	public static boolean isWidgetActive(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getBoolean(IS_WIDGET_ACTIVE, false);
	}

	public static void updateWidget(Context context) {

		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH) + 1;
		int year = calendar.get(Calendar.YEAR);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		final String date = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);

		// query data from db
		ScoresDBHelper helper = new ScoresDBHelper(context);
		Cursor c = helper.getReadableDatabase().query(DatabaseContract.SCORES_TABLE, null,
				DatabaseContract.scores_table.DATE_COL + "= ?", new String[]{date}, null, null, null);

		List<Score> scores = new ArrayList<>();

		while (c != null && c.moveToNext()) {
			scores.add(new Score(
					c.getString(ScoresAdapter.COL_HOME),
					c.getString(ScoresAdapter.COL_AWAY),
					c.getString(ScoresAdapter.COL_MATCHTIME),
					Utilies.getScores(c.getInt(ScoresAdapter.COL_HOME_GOALS), c.getInt(ScoresAdapter.COL_AWAY_GOALS)),
					Utilies.getTeamCrestByTeamName(c.getString(ScoresAdapter.COL_HOME)),
					Utilies.getTeamCrestByTeamName(c.getString(ScoresAdapter.COL_AWAY))
			));
		}

		if (c == null) {
			return;
		} else {
			c.close();
		}
		helper.getReadableDatabase().close();

		// set fields
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, FootballAppWidgetProvider.class));
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget);

		List<Score> widgetScores = new ArrayList<>(3);

		// filter games
		for (Score score : scores) {
			String[] tkn = score.time.split(":");
			final int startHour = Integer.parseInt(tkn[0]);
			final int startMin = Integer.parseInt(tkn[1]);

			// show the most recent
			final int currentTimeMin = hour * 60 + minute;
			final int matchTimeMin = startHour * 60 + startMin;

			if (currentTimeMin < matchTimeMin + MATCH_LENGTH) {
				widgetScores.add(score);
				if (widgetScores.size() == 3) {
					break;
				}
			}
		}

		// sort by ASC
		Collections.reverse(widgetScores);

		remoteViews.setViewVisibility(R.id.appwidget_match1, View.GONE);
		remoteViews.setViewVisibility(R.id.appwidget_match2, View.GONE);
		remoteViews.setViewVisibility(R.id.appwidget_match3, View.GONE);

		if (widgetScores.isEmpty()) {
			// currently no games
			remoteViews.setViewVisibility(R.id.appwidget_no_match, View.VISIBLE);

		} else {
			// set game data
			remoteViews.setViewVisibility(R.id.appwidget_no_match, View.GONE);

			final int size = widgetScores.size();
			int index = size - 1;
			switch (size) {
			case 3:
				remoteViews.setTextViewText(R.id.appwidget_start_time1, widgetScores.get(index).time);
				remoteViews.setTextViewText(R.id.appwidget_home1, widgetScores.get(index).homeName);
				remoteViews.setTextViewText(R.id.appwidget_away1, widgetScores.get(index).awayName);
				remoteViews.setTextViewText(R.id.appwidget_score1, widgetScores.get(index).score);
				remoteViews.setViewVisibility(R.id.appwidget_match1, View.VISIBLE);
				index--;
			case 2:
				remoteViews.setTextViewText(R.id.appwidget_start_time2, widgetScores.get(index).time);
				remoteViews.setTextViewText(R.id.appwidget_home2, widgetScores.get(index).homeName);
				remoteViews.setTextViewText(R.id.appwidget_away2, widgetScores.get(index).awayName);
				remoteViews.setTextViewText(R.id.appwidget_score2, widgetScores.get(index).score);
				remoteViews.setViewVisibility(R.id.appwidget_match2, View.VISIBLE);
				index--;
			case 1:
				remoteViews.setTextViewText(R.id.appwidget_start_time3, widgetScores.get(index).time);
				remoteViews.setTextViewText(R.id.appwidget_home3, widgetScores.get(index).homeName);
				remoteViews.setTextViewText(R.id.appwidget_away3, widgetScores.get(index).awayName);
				remoteViews.setTextViewText(R.id.appwidget_score3, widgetScores.get(index).score);
				remoteViews.setViewVisibility(R.id.appwidget_match3, View.VISIBLE);
			}
		}

		Intent intent = new Intent(context, FootballAppWidgetProvider.class);
		intent.setAction(ACTION_START_APP);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		remoteViews.setOnClickPendingIntent(R.id.appwidget_layout, pendingIntent);

		appWidgetManager.updateAppWidget(ids, remoteViews);

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		if (intent.getAction().equals(ACTION_START_APP)) {
			Intent i = new Intent(context, MainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(i);
		}
	}
}

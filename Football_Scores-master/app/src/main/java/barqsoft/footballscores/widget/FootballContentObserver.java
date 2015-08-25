package barqsoft.footballscores.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import barqsoft.footballscores.DatabaseContract;

public class FootballContentObserver extends ContentObserver {

	private static volatile Handler handler;
	private final Context context;

	public static void register(Context context) {
		if (handler == null) {
			handler = new Handler(Looper.getMainLooper());
			context.getContentResolver().registerContentObserver(DatabaseContract.scores_table.buildScoreWithDate(), true,
					new FootballContentObserver(handler, context));
		}
	}

	public FootballContentObserver(Handler handler, Context context) {
		super(handler);
		this.context = context;
	}

	@Override
	public void onChange(boolean selfChange, Uri uri) {
		super.onChange(selfChange, uri);
		if (FootballAppWidgetProvider.isWidgetActive(context)) {
			FootballAppWidgetProvider.updateWidget(context);
		}
	}
}

package barqsoft.footballscores;

import android.app.Application;

import com.facebook.stetho.Stetho;

public class FootballApp extends Application {

	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
			Stetho.initialize(
					Stetho.newInitializerBuilder(this)
							.enableDumpapp(
									Stetho.defaultDumperPluginsProvider(this))
							.enableWebKitInspector(
									Stetho.defaultInspectorModulesProvider(this))
							.build());
		}
	}
}

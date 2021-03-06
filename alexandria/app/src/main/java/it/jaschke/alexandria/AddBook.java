package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;

public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
	private static final int REQUEST_CODE = 2;
	private EditText ean;
	private String currentEan;
	private final int LOADER_ID = 1;
	private View rootView;
	private final String EAN_CONTENT = "eanContent";
	private static final String SCAN_FORMAT = "scanFormat";
	private static final String SCAN_CONTENTS = "scanContents";

	private String mScanFormat = "Format:";
	private String mScanContents = "Contents:";

	public AddBook() {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (ean != null) {
			outState.putString(EAN_CONTENT, ean.getText().toString());
		}
		clearFields();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
		ean = (EditText) rootView.findViewById(R.id.ean);

		ean.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				//no need
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//no need
			}

			@Override
			public void afterTextChanged(Editable s) {
				String ean = s.toString();
				//catch isbn10 numbers
				if (ean.length() == 10 && !ean.startsWith("978")) {
					ean = "978" + ean;

				} else if (ean.length() < 13) {
					return;
				}

				if (ean.equals(currentEan)) {
					return;
				}

				currentEan = ean;

				//Once we have an ISBN, start a book intent
				Intent bookIntent = new Intent(getActivity(), BookService.class);
				bookIntent.putExtra(BookService.EAN, ean);
				bookIntent.setAction(BookService.FETCH_BOOK);
				getActivity().startService(bookIntent);

				AddBook.this.restartLoader();
			}
		});

		rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				startActivityForResult(intent, REQUEST_CODE);
			}
		});

		rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ean.setText("");
			}
		});

		rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent bookIntent = new Intent(getActivity(), BookService.class);
				bookIntent.putExtra(BookService.EAN, ean.getText().toString());
				bookIntent.setAction(BookService.DELETE_BOOK);
				getActivity().startService(bookIntent);
				ean.setText("");
			}
		});

		if (savedInstanceState != null) {
			ean.setText(savedInstanceState.getString(EAN_CONTENT));
			ean.setHint("");
		}

		return rootView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {

				final String contents = data.getStringExtra("SCAN_RESULT");

				// set as ean which will trigger the listener
				if (!TextUtils.isEmpty(contents)) {
					ean.setText(contents);
				} else {
					Toast.makeText(getActivity(), getString(R.string.code_invalid), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void restartLoader() {
		try {
			if (!getActivity().isFinishing()) {
				getLoaderManager().restartLoader(LOADER_ID, null, this);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(
				getActivity(),
				AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(currentEan == null ? "" : currentEan)),
				null,
				null,
				null,
				null
		);
	}

	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
		if (data == null || !data.moveToFirst()) { // data might be null?
			return;
		}

		String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
		((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

		String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
		((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

		String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

		// a book without author!
		if (!TextUtils.isEmpty(authors)) {
			String[] authorsArr = authors.split(",");
			((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
			((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));

		} else {
			((TextView) rootView.findViewById(R.id.authors)).setText("");
		}

		String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
		if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
			new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
			rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
		}

		String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
		((TextView) rootView.findViewById(R.id.categories)).setText(categories);

		rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
		rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);

		data.close();
	}

	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

	}

	private void clearFields() {
		if (rootView != null) {
			((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
			((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
			((TextView) rootView.findViewById(R.id.authors)).setText("");
			((TextView) rootView.findViewById(R.id.categories)).setText("");
			rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
			rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
			rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activity.setTitle(R.string.scan);
	}
}

package eu.faircode.xlua;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import eu.faircode.xlua.api.xmock.XMockQuery;
import eu.faircode.xlua.api.config.XMockConfigSetting;
import eu.faircode.xlua.api.config.XMockConfigConversions;
import eu.faircode.xlua.api.config.XMockConfig;
import eu.faircode.xlua.api.xmock.call.PutMockConfigCommand;
import eu.faircode.xlua.utilities.BundleUtil;
import eu.faircode.xlua.utilities.FileDialogUtil;

public class FragmentConfig extends Fragment {
    private final static String TAG = "XLua.FragmentConfig";

    private AdapterConfig rvConfigAdapter;
    private Spinner spConfigSelection;
    private ArrayAdapter<XMockConfig> spConfigs;

    private Button btApply;
    private Button btExport;

    private Button btSave;
    private Button btImport;

    private FloatingActionButton flConfigActions;

    private static final int PICK_FILE_REQUEST_CODE = 1; // This is a request code you define to identify your request
    private static final int PICK_FOLDER_RESULT_CODE = 2;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public View onCreateView(
            final @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if(DebugUtil.isDebug())
            Log.i(TAG, "FragmentConfig.onCreateView Enter");

        final View main = inflater.inflate(R.layout.configeditor, container, false);

        flConfigActions = main.findViewById(R.id.flConfigOptions);
        flConfigActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                View configOpsView = inflater.inflate(R.layout.configops, (ViewGroup) main, false);

                // Add the inflated view to the main view
                ((ViewGroup) main).addView(configOpsView);
                //Show the view "configops.xml" over the "main" view code here
            }
        });

        //Buttons
        btSave = main.findViewById(R.id.btSaveConfig);
        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = rvConfigAdapter.getConfigName();
                List<XMockConfigSetting> settings = rvConfigAdapter.getEnabledSettings();

                final XMockConfig config = new XMockConfig();
                config.setName(name);
                config.setSettings(XMockConfigConversions.listToHashMapSettings(settings, false));
                config.orderSettings(true);

                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        final Bundle ret = PutMockConfigCommand.invoke(getContext(), config);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void run() {
                                String messageResult = BundleUtil.readResultStatusMessage(ret);
                                Toast.makeText(getContext(), messageResult, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

            }
        });

        btApply = main.findViewById(R.id.btApplyConfig);
        btApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DebugUtil.isDebug())
                    Log.i(TAG, "Applying Settings from config=" + rvConfigAdapter.getConfigName());

                rvConfigAdapter.applyConfig(getContext(), null);
                Toast.makeText(getContext(), "Finished Applying Settings!", Toast.LENGTH_LONG).show();
            }
        });

        btExport = main.findViewById(R.id.btExportConfig);
        btExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                try {
                    startActivityForResult(intent, PICK_FOLDER_RESULT_CODE);
                } catch (Exception e) {
                    Log.i(TAG, "Open Directory Error: " + e);
                    Toast.makeText(getContext(), "An error occurred while opening the directory picker.", Toast.LENGTH_LONG).show();
                }
            }
        });

        btImport = main.findViewById(R.id.btImportConfig);
        btImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*"); // Use "image/*" for images, "application/pdf" for PDF, etc.
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST_CODE);
                } catch (Exception e) {
                    Log.i(TAG, "Open File Error: " + e);
                    Toast.makeText(getContext(), "An error occurred while opening target Config File.", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Buttons

        if(DebugUtil.isDebug())
            Log.i(TAG, "Creating the Drop Down for Configs Fragment Config");

        //Start of Drop Down
        spConfigs = new ArrayAdapter<>(Objects.requireNonNull(getContext()), android.R.layout.simple_spinner_item);
        spConfigs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if(DebugUtil.isDebug())
            Log.i(TAG, "Created the Empty Array for Configs Fragment Config");

        spConfigSelection = main.findViewById(R.id.spConfigEdit);
        spConfigSelection.setTag(null);
        spConfigSelection.setAdapter(spConfigs);
        spConfigSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateSelection(); }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                updateSelection();
            }

            private void updateSelection() {
                XMockConfig selected = (XMockConfig) spConfigSelection.getSelectedItem();
                String configName = (selected == null ? null : selected.getName());
                if(DebugUtil.isDebug())
                    Log.i(TAG, "CONFIG SELECTED=" + configName);

                if (configName == null ? spConfigSelection.getTag() != null : !configName.equals(spConfigSelection.getTag()))
                    spConfigSelection.setTag(configName);

                if(selected != null)
                    rvConfigAdapter.set(selected);
            }
        });


        if(DebugUtil.isDebug())
            Log.i(TAG, "Created Configs Drop Down, Getting Rotate View For Config Settings, Fragment Config");

        RecyclerView rvSettings = main.findViewById(R.id.rvConfigSettings);
        rvSettings.setVisibility(View.VISIBLE);
        rvSettings.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean onRequestChildFocus(RecyclerView parent, RecyclerView.State state, View child, View focused) {
                return true;
            }
        };

        if(DebugUtil.isDebug())
            Log.i(TAG, "Created Layout Settings for Config Settings, Fragment Config");

        llm.setAutoMeasureEnabled(true);
        rvSettings.setLayoutManager(llm);
        rvConfigAdapter = new AdapterConfig();
        rvSettings.setAdapter(rvConfigAdapter);

        List<XMockConfig> configs = new ArrayList<>(XMockQuery.getConfigs(getContext(), true, true));
        pushConfigs(configs);

        if(DebugUtil.isDebug())
            Log.i(TAG, "Created the Layout for Config Settings, Fragment Config, leaving now...");

        return main;
    }

    public void pushConfig(XMockConfig config) {
        spConfigs.add(config);
        spConfigs.notifyDataSetChanged();
    }

    public void pushConfigs(List<XMockConfig> configs) {
        if(DebugUtil.isDebug())
            Log.i(TAG, "[pushConfigs] configs size=" + configs.size());

        spConfigs.clear();
        spConfigs.addAll(configs);
        spConfigs.notifyDataSetChanged(); // Ensure this is here
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data == null)
            return;

        Uri selectedFileUri = data.getData();
        if(selectedFileUri == null || resultCode != Activity.RESULT_OK)
            return;

        switch (requestCode) {
            case PICK_FILE_REQUEST_CODE:
                String mimeType = Objects.requireNonNull(getContext()).getContentResolver().getType(selectedFileUri);
                if ("application/json".equals(mimeType) || "text/plain".equals(mimeType)) {
                    final XMockConfig config = FileDialogUtil.readPhoneConfig(getContext(), selectedFileUri);
                    if(config == null)
                        Toast.makeText(getContext(), "Failed Read Config File: " + selectedFileUri.getPath(), Toast.LENGTH_SHORT).show();
                    else {
                        String configName = config.getName();
                        for(int i = 0; i < spConfigs.getCount(); i++) {
                            XMockConfig conf = spConfigs.getItem(i);
                            if(configName.equals(conf.getName())) {
                                configName += "-" + ThreadLocalRandom.current().nextInt(10000,999999999);
                                config.setName(configName);
                                break;
                            }
                        }

                        pushConfig(config);
                        Toast.makeText(getContext(), "Read Config: " + configName, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "File type for Parsing is not Supported: " + mimeType, Toast.LENGTH_SHORT).show();
                }
                break;
            case PICK_FOLDER_RESULT_CODE:
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                Objects.requireNonNull(getContext()).getContentResolver().takePersistableUriPermission(selectedFileUri, takeFlags);

                if(!FileDialogUtil.saveConfigSettings(getContext(), selectedFileUri, rvConfigAdapter))
                    Toast.makeText(getContext(), "Failed to Save File: " + rvConfigAdapter.getConfigName(), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getContext(), "Saved File: " + rvConfigAdapter.getConfigName(), Toast.LENGTH_SHORT).show();

                break;
        }
    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onPause() {
        super.onPause();
    }
}

package com.example.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;

public class SummaryGeneratorActivity extends AppCompatActivity {

    private EditText etNotesContent;
    private Button btnSelectPhoto, btnGenerateSummary;
    private TextView tvSummaryResult;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_generator);

        setupToolbar();
        initViews();
        setupListeners();
        setupImagePicker();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("活动总结助手");
        }
    }

    private void initViews() {
        etNotesContent = findViewById(R.id.et_notes_content);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        btnGenerateSummary = findViewById(R.id.btn_generate_summary);
        tvSummaryResult = findViewById(R.id.tv_summary_result);
        progressBar = findViewById(R.id.progress_bar);
        tvSummaryResult.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                            // 【修改点】不再调用 ML Kit，而是调用我们新的大模型方法
                            performOcrWithApiModel(bitmap);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        btnSelectPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnGenerateSummary.setOnClickListener(v -> {
            String content = etNotesContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入内容或从照片识别", Toast.LENGTH_SHORT).show();
                return;
            }
            generateSummary(content);
        });
    }

    /**
     * 【新方法】使用大模型API进行OCR
     * @param bitmap
     */
    private void performOcrWithApiModel(Bitmap bitmap) {
        // 禁用按钮并显示提示
        btnSelectPhoto.setEnabled(false);
        btnGenerateSummary.setEnabled(false);
        etNotesContent.setText("正在通过云端识别图片文字，请稍候...");

        new Thread(() -> {
            String recognizedText = AiApiUtils.getTextFromImage(bitmap);
            // 回到主线程更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                etNotesContent.setText(recognizedText);
                btnSelectPhoto.setEnabled(true);
                btnGenerateSummary.setEnabled(true);
            });
        }).start();
    }

    // generateSummary 方法保持不变
    private void generateSummary(String content) {
        progressBar.setVisibility(View.VISIBLE);
        tvSummaryResult.setText("正在生成摘要，请稍候...");
        btnGenerateSummary.setEnabled(false);
        btnSelectPhoto.setEnabled(false);

        new Thread(() -> {
            String summary = AiApiUtils.generateActivitySummary(content);
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                tvSummaryResult.setText(summary);
                btnGenerateSummary.setEnabled(true);
                btnSelectPhoto.setEnabled(true);
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // 【旧的performOcr方法可以删除了，这里注释掉】
    /*
    private void performOcr(Bitmap bitmap) {
        // ... ML Kit code ...
    }
    */
}
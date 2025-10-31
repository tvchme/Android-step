package com.example.stepcounter;// SetStepGoalActivity.java
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SetStepGoalActivity extends AppCompatActivity {
    private EditText goalEditText;
    private StepGoalManager stepGoalManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_step_goal);

        stepGoalManager = new StepGoalManager(this);
        goalEditText = findViewById(R.id.et_goal);
        Button saveButton = findViewById(R.id.btn_save);

        // 显示当前目标
        int currentGoal = stepGoalManager.getDailyGoal();
        goalEditText.setText(String.valueOf(currentGoal));

        saveButton.setOnClickListener(v -> saveGoal());
    }

    private void saveGoal() {
        String goalStr = goalEditText.getText().toString().trim();
        if (goalStr.isEmpty()) {
            Toast.makeText(this, "请输入步数目标", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int goal = Integer.parseInt(goalStr);
            if (goal <= 0) {
                Toast.makeText(this, "目标步数必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }

            stepGoalManager.saveDailyGoal(goal);
            Toast.makeText(this, "目标设置成功", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }
}
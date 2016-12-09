package ag.ndn.ndnoverwifidirect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

public class LandingActivity extends AppCompatActivity {

    private static final String TAG = "LandingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI elements
        Button chooseProducerBtn = (Button) findViewById(R.id.choose_producer_btn);
        Button chooseConsumerBtn = (Button) findViewById(R.id.choose_consumer_btn);
        Button connectToPeerBtn = (Button) findViewById(R.id.connect_button);

        // bind events
        chooseConsumerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent consumerIntent = new Intent(LandingActivity.this, ConsumerActivity.class);
                startActivity(consumerIntent);
            }
        });

        chooseProducerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent producerIntent = new Intent(LandingActivity.this, ProducerActivity.class);
                startActivity(producerIntent);
            }
        });

        connectToPeerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent connectIntent = new Intent(LandingActivity.this, ConnectActivity.class);
                startActivity(connectIntent);
            }
        });

        // init controller
        NDNController.getInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

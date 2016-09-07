package ag.ndn.ndnoverwifidirect;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class LandingActivity extends AppCompatActivity {

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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

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
                Intent connectIntent = new Intent(LandingActivity.this, MainActivity.class);
                startActivity(connectIntent);
            }
        });
    }

}

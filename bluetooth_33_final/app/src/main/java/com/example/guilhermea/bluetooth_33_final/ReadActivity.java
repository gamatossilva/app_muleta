package com.example.guilhermea.bluetooth_33_final;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/** Classe que mostra o conteúdo do último arquivo editado
 *  Obtém o diretório e o nome do arquivo através da chave "RECORDED_FILE"
 */

public class ReadActivity extends Activity{
    private TextView tvReadText;
    private Constants.Type type;
    private String fileName;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        tvReadText = (TextView) findViewById(R.id.tvReadText);
        type = (Constants.Type) getIntent().getSerializableExtra(Constants.STORAGE_TYPE);
        fileName = getIntent().getStringExtra(Constants.RECORDED_FILE);

        try{
            if (type == Constants.Type.INTERNAL){
                ReadInternal();
            } else{
                ReadExternal();
            }
        } catch (IOException e){
            Toast.makeText(getApplicationContext(), "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void ReadInternal() throws IOException{
        String status = Environment.getExternalStorageState();

        if (!status.equals(Environment.MEDIA_MOUNTED)){
            throw new IOException("SD Card não montado ou não disponível!!!");
        }
        Scanner scanner = new Scanner( new File(fileName));
        Toast.makeText(getApplicationContext(), "Arquivo lido em: " + fileName, Toast.LENGTH_SHORT).show();

        try{
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()){
                String line = scanner.nextLine();
                sb.append(line).append(System.lineSeparator());
            }
            tvReadText.setMovementMethod(new ScrollingMovementMethod());
            tvReadText.setText(sb.toString());
        } finally {
            scanner.close();
        }
    }

    private void ReadExternal() throws IOException{
        String status = Environment.getExternalStorageState();

        if (!status.equals(Environment.MEDIA_MOUNTED)){
            throw new IOException("SD Card não montado ou não disponível!!!");
        }

        Scanner scanner = new Scanner( new File(fileName));
        Toast.makeText(getApplicationContext(), "Arquivo lido em: " + fileName, Toast.LENGTH_SHORT).show();

        try{
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()){
                String line = scanner.nextLine();
                sb.append(line).append(System.lineSeparator());
            }
            tvReadText.setText(sb.toString());
        } finally {
            scanner.close();
        }
    }
}

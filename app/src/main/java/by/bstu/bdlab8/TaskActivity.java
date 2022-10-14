package by.bstu.bdlab8;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class TaskActivity extends AppCompatActivity {

    private String choseDate;
    public static ArrayList<Task> currentTasks;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        Bundle extras = getIntent().getExtras();
        choseDate = extras.getString("SelectedDate");

        TextView dateView = findViewById(R.id.editDateView);
        dateView.setText(choseDate);

        Spinner spinner = findViewById(R.id.editSpinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Task.Categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        ListView taskList = findViewById(R.id.taskList);
        registerForContextMenu(taskList);

        taskList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                Task selectedItem = currentTasks.get(position);
                OpenTaskEditor(selectedItem);
            }
        });

        currentTasks = new ArrayList<>();
        for (Task t: Task.Tasks) {
            if(t.Date.equals(choseDate)){
                currentTasks.add(t);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, currentTasks);
        taskList.setAdapter(adapter);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_category_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete:
                deleteTask(info.position);
                FragmentManager manager = getSupportFragmentManager();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void addNewTask(View view) {
        if(Task.Tasks.size() >= 20){
            Toast toast = Toast.makeText(this, "Задач не может быть больше 20", Toast.LENGTH_LONG);
            toast.show();
        }
        else if(currentTasks.size() >= 5){
            Toast toast = Toast.makeText(this, "Задач на текущую дату не может быть больше 5", Toast.LENGTH_LONG);
            toast.show();
        }
        else{
            Task newTask = new Task();
            newTask.Date = choseDate;
            EditText taskInput = findViewById(R.id.editInput);
            newTask.Title = String.valueOf(taskInput.getText());
            if(newTask.Title.length() <= 0){
                Toast toast = Toast.makeText(this, "Введите название!", Toast.LENGTH_LONG);
                toast.show();
            }
            else{
                Spinner spinner = findViewById(R.id.editSpinner);
                newTask.Category = String.valueOf(spinner.getSelectedItem());
                Task.Tasks.add(newTask);
                currentTasks.add(newTask);

                ListView taskList = findViewById(R.id.taskList);
                ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, currentTasks);
                taskList.setAdapter(adapter);
                Task.SerializeTasks(getApplicationContext());
            }
        }
    }

    public void deleteTask(int index){
        Task t = currentTasks.get(index);
        currentTasks.remove(t);
        Task.Tasks.remove(t);
        ListView taskList = findViewById(R.id.taskList);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, currentTasks);
        taskList.setAdapter(adapter);
        Task.SerializeTasks(getApplicationContext());
        Toast toast = Toast.makeText(this, "Задача удалена", Toast.LENGTH_LONG);
        toast.show();
    }

    public void OpenTaskEditor(Task task){
        Intent intent = new Intent(this, EditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("Title", task.Title);
        intent.putExtra("Category", task.Category);
        intent.putExtra("Date", task.Date);
        startActivity(intent);
    }

    public void xsltClick(View view) {
        try{
            String xslt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "\n" +
                    "<xsl:stylesheet version=\"1.0\"\n" +
                    "   xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "\n" +
                    "<xsl:template match=\"/\">\n" +
                    "    <Tasks><xsl:for-each select=\"Tasks/Task\">\n" +
                    "    <xsl:sort select=\"@Category\"/>\n" +
                    "    <xsl:if test=\"@Date='"+ choseDate + "'\">\n<Task>" +
                    "        <Category><xsl:value-of select=\"@Category\"/></Category>\n" +
                    "        <Date><xsl:value-of select=\"@Date\"/></Date>\n" +
                    "        <Title><xsl:value-of select=\"@Title\"/></Title>\n" +
                    "    </Task></xsl:if>\n" +
                    "    </xsl:for-each>\n</Tasks>" +
                    "    <xsl:apply-templates/>\n" +
                    "</xsl:template>\n" +
                    "\n" +
                    "</xsl:stylesheet> ";
            File f = new File(getFilesDir(), "lab.xslt");
            FileWriter fw = new FileWriter(f, false);
            fw.write(xslt);
            fw.close();
            FileInputStream xmlf = openFileInput("tasks.xml");
            FileInputStream xslf = openFileInput("lab.xslt");
            FileOutputStream txt = openFileOutput("res.xml", MODE_PRIVATE);
            TransformerFactory tf = TransformerFactory.newInstance();
            Source xsltsrc = new StreamSource(xslf);
            Source xmlsrc = new StreamSource(xmlf);
            Transformer t = tf.newTransformer(xsltsrc);
            t.transform(xmlsrc, new StreamResult(txt));
            Toast.makeText(this, "TXT создан!", Toast.LENGTH_SHORT).show();
        }
        catch (Exception ex){
            Log.d("LAB88", ex.getMessage());
        }

    }
}
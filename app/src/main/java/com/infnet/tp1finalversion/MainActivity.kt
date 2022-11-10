package com.infnet.tp1finalversion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.android.material.snackbar.Snackbar
import com.infnet.tp1finalversion.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    fun obterDiretorio(diretorio: String, criar: Boolean): File {
        var dirArq = getExternalFilesDir(null)!!.path + "/" + diretorio
        var dirFile = File(dirArq)
        if(!dirFile.exists()&&(!criar||!dirFile.mkdirs()))
            throw Exception("Diretório indisponível")
        return dirFile
    }

//    fun removerDiretorio(diretorio: String){
//        var dirArq = getExternalFilesDir(null)!!.path + "/" + diretorio
//        var dirFile = File(dirArq)
//        if(!dirFile.exists() || !dirFile.isDirectory ||
//            !dirFile.listFiles().isEmpty())
//            throw Exception("Não é possível remover")
//        dirFile.delete()
//    }

    private fun obterRegistros():List<File>{
        var diretorio = obterDiretorio("registros",true)
        return diretorio.listFiles().filter {
                t -> t.name.endsWith(".crd")
        }
    }

    // Gravação e leitura criptografadas
    private fun gravarRegistro(texto: String): File {
        var diretorio = obterDiretorio("registros",false)
        val chaveMestra = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        var registro = File(diretorio.path+"/"+ Date().time+".note")
        var encryptedOut = EncryptedFile.Builder(
            applicationContext, registro, chaveMestra,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileOutput()
        val pw = PrintWriter(encryptedOut)
        pw.println(texto)
        pw.flush()
        encryptedOut.close()
        return registro
    }

    fun lerRegistro(nomeRegistro: String): String{
        var diretorio = obterDiretorio("registros",false)
        val chaveMestra = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        var registro = File(diretorio.path+"/"+nomeRegistro)
        var encryptedIn = EncryptedFile.Builder(
            applicationContext, registro, chaveMestra,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput()
        val br = BufferedReader(InputStreamReader(encryptedIn))
        val sb = StringBuffer()
        br.lines().forEach{ t -> sb.append(t+"\n") }
        encryptedIn.close()
        return sb.toString()
    }

    // Final da leitura e gravação
    private lateinit var recyclerView: RecyclerView
    private lateinit var arquivoAdapter: ArquivoAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewManager = LinearLayoutManager(this)
        arquivoAdapter = ArquivoAdapter(ArrayList<File>(obterRegistros()))
        recyclerView = findViewById<RecyclerView>(R.id.recView).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = arquivoAdapter
        }

        editText = findViewById(R.id.txtPlaca)

        // Código do outro onCreate
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        layout = binding.mainLayout

        setContentView(view)

        setup()
    }

    fun clickAdd(view: View){
        arquivoAdapter.addRegistro(gravarRegistro(editText.text.toString()))
        editText.setText("")
    }

    fun clickLer(view: View){
        var nomeRegistro = (view as TextView).text.toString()
        editText.setText(lerRegistro(nomeRegistro))
    }

    // Código do GPS
    private lateinit var layout: View
    private  lateinit var binding: ActivityMainBinding

    private var permissaoLocalizacao = false
    private var permissaoEscrita = false
    private var permissaoLeitura = false


    //Variaveis reponsaveis por lançar as permissioes
    private lateinit var singlePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>


    fun setup(){
        verificaSeExisteSd()
        setupLancarPermissoes()
        setupClickBotao()
    }

    companion object{
        fun verificaSeExisteSd(): Boolean{
            if(Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()){
                Log.i( "RW","SD montado")
                return true
            }
            return false
        }
    }

    private fun setupClickBotao(){
        binding.btnRegistrar.setOnClickListener {
            toast("Cliquei no botão")
            verificarPermissao()
        }
    }

    fun setupLancarPermissoes(){
        singlePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
                permissaoGarantida: Boolean ->
            if(permissaoGarantida){
                toast("Permissão Concedida")
            }
            else{
                toast("Permissão Negada.")
            }
        }

        multiplePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissaoLocalizacao = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: permissaoLocalizacao
            permissaoEscrita = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: permissaoEscrita
            permissaoLeitura = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: permissaoLeitura
        }
    }

    fun verificarPermissao(){
        permissaoLocalizacao = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        permissaoEscrita = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        permissaoLeitura = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        val requestPermissions: MutableList<String> = ArrayList()

        if(!permissaoLocalizacao){
            requestPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if(!permissaoEscrita){
            requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!permissaoLeitura){
            requestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if(requestPermissions.isNotEmpty()){
            when{
                (!permissaoLocalizacao && !permissaoEscrita && !permissaoLeitura) -> {perguntaTodasPermissoes(requestPermissions)}

                (!permissaoLocalizacao && permissaoEscrita && permissaoLeitura) -> {perguntaPermissaoLocalizacao()}

                (permissaoLocalizacao && !permissaoEscrita && permissaoLeitura) -> {perguntaPermissaoEscrita()}

                (permissaoLocalizacao && permissaoEscrita && permissaoLeitura) -> {perguntaPermissaoLeitura()}

            }
        }
        else{
            startActivity(Intent(this,MainActivity::class.java))
        }
    }

    private fun perguntaPermissaoEscrita(){
        layout.showSnackbar(binding.mainLayout,"Precisamos do acesso da sua localização para registra-la", Snackbar.LENGTH_INDEFINITE,"OK"){
            singlePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun perguntaPermissaoLeitura(){
        layout.showSnackbar(binding.mainLayout,"Precisamos do acesso da sua localização para registra-la", Snackbar.LENGTH_INDEFINITE,"OK"){
            singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun perguntaPermissaoLocalizacao(){
        layout.showSnackbar(binding.mainLayout,"Precisamos do acesso da sua localização para registra-la", Snackbar.LENGTH_INDEFINITE,"OK"){
            singlePermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun perguntaTodasPermissoes(requestPermissions: MutableList<String>){
        layout.showSnackbar(binding.mainLayout,"Precisamos do acesso da sua localização para registra-la", Snackbar.LENGTH_INDEFINITE,"OK"){
            multiplePermissionLauncher.launch(requestPermissions.toTypedArray())
        }
    }


    // Código do Main Activity 2
    private lateinit var locationManager: LocationManager

    private fun getLocation(){
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if((ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)){

        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5f,this)
    }

//    override fun onLocationChanged(location: Location) {
//        binding.cntLat.text = "%.4f".format(location.latitude)
//        binding.cntLon.text = "%.4f".format(location.longitude)
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getData(){
        val dateTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val timeFormat = DateTimeFormatter.ofPattern("HH:MM:SS")
//        binding.cntData.text = dateTime.format(dateFormat)
//        binding.cntHora.text = dateTime.format(timeFormat)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun criarEescreverNoAquivo(){

        Log.i("RW","Iniciando a escrita no arquivo")

        if(MainActivity.verificaSeExisteSd()){
            val dateTime = LocalDateTime.now()
            val dateFormat = dateTime.format(DateTimeFormatter.ofPattern("dd_MM_yyyy"))
            val timeFormat = dateTime.format(DateTimeFormatter.ofPattern("HH:MM"))

            val conteudoArq = "${binding.cntLat.text}\n${binding.cntLon.text}"
            val nomeArq = "${dateFormat}_${timeFormat}"
            val diretorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val pathArq = "${diretorio}/${nomeArq}.txt"
            Log.i("RW", "aa $pathArq")
            val file = File(pathArq)

            try {
                diretorio.mkdirs()
                //file.createNewFile()
                file.writeText("conteudoArq")
                Toast.makeText(this,"Escrita realizada com sucesso", Toast.LENGTH_LONG).show()
            }
            catch (e: java.lang.Exception){
                Log.e("RW", "Exceção: $e")
            }
        }
        else{
            Toast.makeText(this, "Não foi possivel reconhecer o SD CARD", Toast.LENGTH_LONG).show()
        }
    }
}
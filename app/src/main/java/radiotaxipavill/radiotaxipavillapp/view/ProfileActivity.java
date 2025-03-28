package radiotaxipavill.radiotaxipavillapp.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import radiotaxipavill.radiotaxipavillapp.R;
import radiotaxipavill.radiotaxipavillapp.components.CircularImageView;
import radiotaxipavill.radiotaxipavillapp.components.LoadingDialog;
import radiotaxipavill.radiotaxipavillapp.components.NavigationHeaderInfo;
import radiotaxipavill.radiotaxipavillapp.controller.ClienteController;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST_CODE = 100;


    private CircularImageView profileImage;
    private ImageButton editImageButton;

    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Inicializar LoadingDialog
        loadingDialog = new LoadingDialog(this);

        // Referencias a los TextView
//        TextView changePhone = findViewById(R.id.ChangePhone);
        TextView changePassword = findViewById(R.id.ChangePassword);

        // Referencias a los inputs
        EditText editDNI = findViewById(R.id.edit_dni);
        EditText editName = findViewById(R.id.edit_name);
        EditText editEmail = findViewById(R.id.edit_email);
        EditText editPhone = findViewById(R.id.edit_phone);

        // Cargar datos iniciales desde SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        editDNI.setText(sharedPreferences.getString("ClienteNumeroDocumento", ""));
        editName.setText(sharedPreferences.getString("ClienteNombre", ""));
        editEmail.setText(sharedPreferences.getString("ClienteEmail", ""));
        editPhone.setText(sharedPreferences.getString("ClienteCelular", ""));

        profileImage = findViewById(R.id.profile_image);
        editImageButton = findViewById(R.id.edit_image_button);

        // Cargar imagen previamente guardada
        loadProfileImage();

        // Configurar el botón para editar la imagen
        editImageButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                showImagePickerDialog();
            } else {
                requestPermissions();
            }
        });

//        // Evento para cambiar número de celular
//        changePhone.setOnClickListener(v -> {
//            Intent intent = new Intent(ProfileActivity.this, VerifyPhoneActivity.class);
//            intent.putExtra("origin", "ProfileActivity");
//            intent.putExtra("action", "changePhoneNumber");
//            startActivity(intent);
//        });

        // Evento para cambiar contraseña
        changePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, VerifyPhoneActivity.class);
            intent.putExtra("origin", "ProfileActivity");
            intent.putExtra("action", "changePassword");
            startActivity(intent);
        });

        // Configurar botón para guardar cambios
        Button editButton = findViewById(R.id.edit_button);
        editButton.setOnClickListener(v -> {
            String clienteId = sharedPreferences.getString("ClienteId", null);
            String clienteNumeroDocumento = editDNI.getText().toString();
            String clienteNombre = editName.getText().toString();
            String clienteEmail = editEmail.getText().toString();
            String clienteCelular = editPhone.getText().toString();

            // Mostrar indicador de carga
            loadingDialog.show();


            // Validar que el ClienteId no sea nulo
            if (clienteId == null) {
                Toast.makeText(ProfileActivity.this, "Error interno, vuelva a iniciar sesión", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamar al controlador para realizar la edición
            new ClienteController().editarCliente(
                    ProfileActivity.this,
                    clienteId,
                    clienteNumeroDocumento,
                    clienteNombre,
                    clienteEmail,
                    clienteCelular,
                    new ClienteController.EditarClienteCallback() {
                        @Override
                        public void onSuccess(String message) {
                            loadingDialog.dismiss();

                            // Mostrar el ArrivalMessageDialog
                            ArrivalMessageDialog arrivalMessageDialog = new ArrivalMessageDialog();
                            arrivalMessageDialog.setDriverName("Los cambios se actualizaron exitosamente.");
                            arrivalMessageDialog.setButtonText("Continuar");
                            arrivalMessageDialog.setOnConfirmClickListener(() -> {
                                // Actualizar SharedPreferences con los nuevos datos
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("ClienteNumeroDocumento", clienteNumeroDocumento);
                                editor.putString("ClienteNombre", clienteNombre);
                                editor.putString("ClienteEmail", clienteEmail);
                                editor.putString("ClienteCelular", clienteCelular);
                                editor.apply();

                                // Refrescar la página del perfil
                                recreate();
                            });
                            arrivalMessageDialog.show(getSupportFragmentManager(), "UpdateSuccessDialog");
                        }


                        @Override
                        public void onFailure(String errorMessage) {

                            loadingDialog.dismiss();

                            Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();

                        }
                    }
            );
        });

        // Obtener referencia del DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Obtener referencia del NavigationView
        navigationView = findViewById(R.id.nav_view);

        // Configurar NavigationView
        setupNavigationView();

        // Obtener referencia del botón de menú
        CardView btnOpenSidebar = findViewById(R.id.btnOpenSidebar);
        if (btnOpenSidebar != null) {
            btnOpenSidebar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(navigationView);
                    }
                }
            });
        }

        // Cargar datos desde SharedPreferences y establecerlos en los campos del formulario
        loadUserData();

        Button deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            // Obtener la URL desde los recursos de strings
            String url = getString(R.string.url_delete_acount);

            // Crear un intent para abrir el navegador
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // Verificar que haya un navegador disponible antes de lanzar el intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No se encontró un navegador disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---- para la imagen ---

    // Verifica si los permisos están concedidos
    private boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Solicita permisos
    private void requestPermissionsWithFallback(String... permissions) {
        if (!hasPermissions(permissions)) {
            boolean shouldShowRationale = false;
            for (String permission : permissions) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                // Explica por qué necesitas los permisos
                showPermissionRationaleDialog(permissions);
            } else {
                // Solicita los permisos directamente
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Muestra un diálogo explicando los permisos
    private void showPermissionRationaleDialog(String... permissions) {
        new AlertDialog.Builder(this)
                .setTitle("Permisos requeridos")
                .setMessage("Esta aplicación necesita permisos para funcionar correctamente. Por favor, actívalos.")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Solo verifica permiso de cámara en Android 10+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            // En versiones anteriores, verifica también permisos de almacenamiento
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Solicita solo permiso de cámara en Android 10+
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Solicita permisos de cámara y almacenamiento en versiones anteriores
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Tomar foto", "Seleccionar de la galería"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cambiar imagen de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap selectedImage = null;

            if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
                selectedImage = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri imageUri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    selectedImage = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (selectedImage != null) {
                saveProfileImage(selectedImage);
                profileImage.setImageBitmap(selectedImage);
                Toast.makeText(this, "Imagen actualizada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfileImage(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "profile_image.png");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProfileImage() {
        try {
            File cacheDir = getCacheDir();
            File imageFile = new File(cacheDir, "profile_image.png");

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                profileImage.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Todos los permisos fueron concedidos
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                // Algunos permisos fueron denegados
                boolean showRationale = false;
                for (String permission : permissions) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        showRationale = true;
                        break;
                    }
                }

                if (!showRationale) {
                    // El usuario marcó "No volver a preguntar"
                    showSettingsRedirectDialog();
                } else {
                    // El usuario solo denegó los permisos (no marcó "No volver a preguntar")
                    requestPermissionsWithFallback();
                    Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showSettingsRedirectDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage("Los permisos han sido denegados. Por favor, actívalos manualmente en Configuración.")
                .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    // fin para la imagen


    private void setupNavigationView() {
        // Configurar el encabezado
        NavigationHeaderInfo.setupHeader(this, navigationView);

        // Manejar los eventos de click en los elementos del menú del NavigationView
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_history) {
                    // Lógica para navegar al historial
                    Intent historyIntent = new Intent(ProfileActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_maps) {
                    // Lógica para navegar al mapa
                    Intent historyIntent = new Intent(ProfileActivity.this, MapActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_points) {
                    // Lógica para navegar a points
                    Intent historyIntent = new Intent(ProfileActivity.this, PointsActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_favorites) {
                    // Lógica para navegar a points
                    Intent historyIntent = new Intent(ProfileActivity.this, FavoritesActivity.class);
                    startActivity(historyIntent);
                } else if (id == R.id.nav_logout) {
                    // Lógica para cerrar sesión
                    SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    Intent logoutIntent = new Intent(ProfileActivity.this, MainActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                }

                // Cerrar el drawer después de seleccionar una opción
                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Obtener los valores almacenados
        String dni = sharedPreferences.getString("ClienteNumeroDocumento", "");
        String email = sharedPreferences.getString("ClienteEmail", "");
        String name = sharedPreferences.getString("ClienteNombre", "");
        String phone = sharedPreferences.getString("ClienteCelular", "");

        // Establecer los valores en los campos del formulario
        EditText editDni = findViewById(R.id.edit_dni);
        EditText editEmail = findViewById(R.id.edit_email);
        EditText editName = findViewById(R.id.edit_name);
        EditText editPhone = findViewById(R.id.edit_phone);

        editDni.setText(dni);
        editEmail.setText(email);
        editName.setText(name);
        editPhone.setText(phone);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getStringExtra("action");

        if ("changePasswordSuccess".equals(action)) {
            Toast.makeText(this, "Contraseña cambiada con éxito", Toast.LENGTH_SHORT).show();
        }
//        } else if ("changePhoneSuccess".equals(action)) {
//            Toast.makeText(this, "Número cambiado con éxito", Toast.LENGTH_SHORT).show();
//        }
    }

}

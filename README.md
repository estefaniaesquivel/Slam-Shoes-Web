# Slam Shoes - Documentación del Proyecto[cite: 9]

<p align="center">
  <img src="logo-slamshoes.png" alt="Slam Shoes Logo" width="300"/>
</p>

**FACULTAD DE MATEMÁTICAS - UNIVERSIDAD AUTÓNOMA DE YUCATÁN (UADY)**[cite: 9]  
**Licenciatura en Ingeniería en Software**[cite: 9]  
**Asignatura:** Desarrollo de Aplicaciones Web[cite: 9]  
**Proyecto Integrador (Primera revisión - 29 de octubre de 2026):** Prototipo funcional de tienda web para el comercio de calzado deportivo orientado al básquetbol[cite: 9].

---

## Integrantes del Equipo[cite: 9]
- **Caamal Tencle Mateo Alejandro**[cite: 9]
- **Dzib Martínez Agustín Emanuel**[cite: 9]
- **Esquivel Bacelis Estefanía**[cite: 9]
- **Morales Caro Axel Adolfo**[cite: 9]

**Profesor:** Juan Pablo Ucán Pech[cite: 9]  
**Repositorio:** [https://github.com/estefaniaesquivel/Slam-Shoes-Web](https://github.com/estefaniaesquivel/Slam-Shoes-Web)[cite: 9]

---

## 1. Nombre de la Aplicación[cite: 9]
**SLAM SHOES.** Aplicación web de comercio de calzado deportivo orientado al básquetbol[cite: 9].

---

## 2. Descripción General del Problema o Necesidad[cite: 9]
Hoy, muchas personas compran tenis a través de redes sociales, mensajes directos o catálogos sueltos[cite: 9]. Esto causa varios problemas:[cite: 9]
* No hay un catálogo único y ordenado: hay que buscar modelo por modelo en distintos lugares[cite: 9].
* Es difícil saber qué tallas y colores hay disponibles y cuánto stock queda[cite: 9].
* No existe un carrito donde la persona reúna varios productos y vea el total antes de decidir[cite: 9].
* Los negocios pequeños no tienen una tienda digital propia con identidad visual y datos organizados[cite: 9].

**Necesidad que atiende:** Slam Shoes ofrece una tienda web con catálogo organizado, cuentas de usuario, carrito de compras y registro de pedidos, accesible desde computadora o celular[cite: 9].  
**Alcance:** Es un prototipo escolar[cite: 9]. Se busca demostrar la arquitectura completa (FrontEnd, BackEnd y base de datos en la nube) y los flujos principales de compra[cite: 9]. Los pagos reales y la logística de envíos quedan fuera del alcance[cite: 9].  
**Referencia de diseño:** La interfaz tomará como inspiración el estilo de tiendas deportivas como Way of Wade, Kicks Own y Anta (inicio de sesión, banners en la página principal de tendencias, navegación clara, menú desplegable, etc.)[cite: 9]. El diseño y los recursos gráficos como el logo serán propios del equipo[cite: 9].

---

## 3. Explicación General de la Funcionalidad Principal[cite: 10]
La aplicación tiene dos tipos de usuario: el cliente, que compra, y el administrador, que gestiona los productos[cite: 10]. Sus módulos son:[cite: 10]

| Módulo | Qué hace |
| :--- | :--- |
| **Inicio (index)** | Página principal con menú de navegación, banner, categorías y productos destacados[cite: 10]. |
| **Autenticación** | Registro e inicio de sesión de usuarios[cite: 10]. Las contraseñas se guardan cifradas (hash)[cite: 10]. |
| **Catálogo** | Lista de productos con filtros por categoría, talla, color y precio, y ordenamiento[cite: 10]. |
| **Detalle de producto** | Fotos, descripción, precio, selección de talla y color, botón de favoritos y agregar al carrito[cite: 10]. |
| **Favoritos** | Lista de los productos seleccionados como favoritos por el usuario[cite: 10]. |
| **Carrito de compras** | Agregar, quitar y cambiar cantidades; cálculo automático del total[cite: 10]. |
| **Pedidos (checkout simulado para el administrador)** | En caso de que el usuario sea administrador, hay un módulo más que captura datos de entrega y registro del pedido en la base de datos[cite: 10]. El pago es simulado[cite: 10]. |
| **Administración de productos** | En caso de que el usuario sea administrador, hay un módulo más que permite dar de alta, editar o dar de baja productos[cite: 10]. |

---

## 4. Requisitos del Software[cite: 10]

### 4.1 Requisitos Funcionales[cite: 10]
| ID | Requisito | Prioridad |
| :--- | :--- | :--- |
| **RF-01** | El sistema permitirá registrar usuarios con nombre, correo y contraseña[cite: 10]. | Alta[cite: 10] |
| **RF-02** | El sistema permitirá iniciar sesión con correo y contraseña[cite: 10]. Las contraseñas se almacenarán cifradas[cite: 10]. | Alta[cite: 10] |
| **RF-03** | El sistema distinguirá dos roles: cliente y administrador[cite: 10]. | Media[cite: 10] |
| **RF-04** | El sistema tendrá una página de inicio con categorías y productos destacados[cite: 10]. | Alta[cite: 10] |
| **RF-05** | El sistema mostrará un catálogo de productos tomados de la base de datos, con filtros y ordenamiento[cite: 10]. | Alta[cite: 10] |
| **RF-06** | El sistema mostrará el detalle de cada producto con sus tallas, colores y existencias[cite: 10, 11]. | Alta[cite: 11] |
| **RF-07** | El usuario podrá agregar productos al carrito, cambiar cantidades, eliminarlos y ver el total[cite: 11]. | Alta[cite: 11] |
| **RF-08** | El usuario autenticado podrá generar un pedido; el sistema guardará el pedido y su detalle[cite: 11]. | Media[cite: 11] |
| **RF-09** | El usuario autenticado podrá consultar su historial de pedidos[cite: 11]. | Baja[cite: 11] |
| **RF-10** | El administrador podrá crear, editar y dar de baja productos[cite: 11]. | Baja[cite: 11] |
| **RF-11** | El BackEnd expondrá una API REST que el FrontEnd consumirá en formato JSON[cite: 11]. | Alta[cite: 11] |

### 4.2 Requisitos No Funcionales[cite: 11]
| ID | Requisito | Categoría |
| :--- | :--- | :--- |
| **RNF-01** | La interfaz será responsiva (computadora, tableta y celular)[cite: 11]. | Usabilidad[cite: 11] |
| **RNF-02** | La navegación entre páginas no recargará el navegador (aplicación de una sola página)[cite: 11]. | Rendimiento[cite: 11] |
| **RNF-03** | FrontEnd y BackEnd estarán separados y solo se comunicarán por HTTP/JSON[cite: 11]. | Arquitectura[cite: 11] |
| **RNF-04** | No se guardarán contraseñas en texto plano ni datos de pago reales[cite: 11]. | Seguridad[cite: 11] |
| **RNF-05** | Las credenciales de la base de datos se configurarán con variables de entorno y no se subirán a GitHub[cite: 11]. | Seguridad[cite: 11] |
| **RNF-06** | El código estará versionado en GitHub, con carpetas separadas por capa[cite: 11]. | Mantenibilidad[cite: 11] |
| **RNF-07** | La aplicación podrá desplegarse en servicios gratuitos de la nube[cite: 11]. | Despliegue[cite: 11] |
| **RNF-08** | La aplicación funcionará en las versiones recientes de Chrome, Edge y Firefox[cite: 11]. | Compatibilidad[cite: 11] |

---

## 5. Base de Datos[cite: 11]
La base de datos es relacional, usa PostgreSQL y estará alojada en Supabase[cite: 11]. Cuenta con siete tablas relacionadas entre sí por llaves foráneas[cite: 11, 12].

### 5.1 Tablas y Campos[cite: 11]
| Tabla | Campos Principales | Propósito |
| :--- | :--- | :--- |
| **usuarios** | id_usuario (PK), nombre, correo (único), contrasena_hash, rol, fecha_registro[cite: 12] | Guarda a clientes y administradores; es la base de la autenticación del sistema[cite: 11, 12]. |
| **categorias** | id_categoria (PK), nombre, descripcion[cite: 12] | Agrupa los productos por tipo (básquetbol, running, casual, etc.)[cite: 12]. |
| **jugadores** | id_jugador (PK), nombre, imagen_url[cite: 12] | Almacena a los atletas profesionales que inspiran los modelos de calzado[cite: 12]. |
| **productos** | id_producto (PK), id_categoria (FK), id_jugador (FK, nulo), nombre, marca, descripcion, precio, imagen_url, activo[cite: 12] | Catálogo general de modelos de calzado deportivo[cite: 12]. |
| **producto_tallas** | id_variante (PK), id_producto (FK), talla, color, stock[cite: 12] | Cada combinación específica de talla y color de un producto, con sus existencias disponibles[cite: 12]. |
| **pedidos** | id_pedido (PK), id_usuario (FK), fecha, total, estado, direccion_envio[cite: 12] | Encabezado general de cada compra realizada por los usuarios[cite: 12]. |
| **pedido_detalle** | id_detalle (PK), id_pedido (FK), id_variante (FK), cantidad, precio_unitario[cite: 12] | Renglones de cada pedido: qué producto/variante específica se compró y en qué cantidad[cite: 12]. |

### 5.2 Relaciones entre Tablas[cite: 12]
| Relación | Tipo | Explicación |
| :--- | :--- | :--- |
| **categorias -> productos** | 1 a N[cite: 12] | Una categoría tiene muchos productos; cada producto pertenece a una categoría[cite: 12]. |
| **jugadores -> productos** | 1 a N[cite: 12] | Un jugador profesional inspira o está asociado a muchos modelos de calzado; cada modelo puede pertenecer a un jugador específico (o ser nulo)[cite: 12]. |
| **productos -> producto_tallas** | 1 a N[cite: 12] | Un producto cuenta con muchas variantes de talla, color y stock[cite: 12]. |
| **usuarios -> pedidos** | 1 a N[cite: 12] | Un usuario registrado puede realizar muchos pedidos a lo largo del tiempo[cite: 12]. |
| **pedidos -> pedido_detalle** | 1 a N[cite: 12] | Un pedido contiene uno o más renglones o líneas de compra[cite: 12]. |
| **producto_tallas -> pedido_detalle** | 1 a N[cite: 12] | Una variante específica de talla y color puede aparecer en los renglones de pedidos de distintos clientes[cite: 12]. |

### 5.3 Diagrama Entidad-Relación[cite: 13]
*(Ver figura en el documento oficial del modelo relacional: **SLAM SHOES MODELO RELACIONAL**)*[cite: 13]

---

## 6. Integración Futura del FrontEnd y el BackEnd[cite: 13]
El FrontEnd (Vue) y el BackEnd (Spring Boot) serán dos aplicaciones independientes que se comunican por una API REST: Vue envía peticiones HTTP y Spring Boot responde con datos en formato JSON[cite: 13]. El BackEnd es el único que habla con la base de datos[cite: 13].

### 6.1 Endpoints Previstos de la API[cite: 13]
| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| **POST** | `/api/auth/registro` | Registra un usuario nuevo[cite: 13]. |
| **POST** | `/api/auth/login` | Valida correo y contraseña y devuelve los datos del usuario y su rol[cite: 13, 14]. |
| **GET** | `/api/productos` | Lista los productos (con filtros opcionales)[cite: 13, 14]. |
| **GET** | `/api/productos/{id}` | Detalle de un producto con sus tallas y stock[cite: 13, 14]. |
| **POST** | `/api/pedidos` | Registra un pedido a partir del carrito[cite: 14]. |
| **GET** | `/api/pedidos/usuario/{id}` | Historial de pedidos de un usuario[cite: 14]. |
| **POST / PUT / DELETE** | `/api/productos` | Alta, edición y baja de productos (administrador)[cite: 14]. |

### 6.2 Pasos de Integración[cite: 14]
1. Crear el proyecto en Supabase[cite: 14].
2. Configurar en el BackEnd la conexión a Supabase con variables y hacer pruebas mediante Bruno API Client para verificar la funcionalidad de la BD[cite: 14].
3. Crear en Spring Boot los controladores, servicios y repositorios de usuarios y productos[cite: 14].
4. Crear en Vue las vistas (inicio, login, registro, catálogo, detalle, carrito) y las rutas[cite: 14].
5. Crear el store del carrito y el de sesión de usuario con Pinia[cite: 14].
6. Mediante la configuración de CORS, conectar las vistas con la API y probar todo en local (FrontEnd en `localhost:5173`, BackEnd en `localhost:8080`)[cite: 14].
7. Desplegar el BackEnd y FrontEnd en Render, y actualizar la configuración de CORS[cite: 14].




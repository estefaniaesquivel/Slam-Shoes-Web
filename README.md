# Slam Shoes - Documentación del Proyecto

<p align="center">
  <img src="./logo-slamshoes.png" alt="Slam Shoes Logo" width="300"/>
</p>

**FACULTAD DE MATEMÁTICAS - UNIVERSIDAD AUTÓNOMA DE YUCATÁN (UADY)**  
**Licenciatura en Ingeniería en Software**  
**Asignatura:** Desarrollo de Aplicaciones Web  
**Proyecto Integrador (Primera Revisión - 29 de octubre de 2026):** Prototipo funcional de tienda web para el comercio de calzado deportivo orientado al básquetbol.

---

## Integrantes del Equipo
* **Dzib Martínez Agustín Emanuel**
* **Esquivel Bacelis Estefanía**
* *[Nombre del integrante 3]*
* *[Nombre del integrante 4]*

**Profesor:** Juan Pablo Ucán Pech  


---

## 1. Descripción General del Problema y Necesidad
La compra de calzado deportivo a través de redes sociales y mensajes directos presenta múltiples deficiencias:
1. **Falta de catálogo organizado:** Búsqueda dispersa modelo por modelo.
2. **Control de stock ineficiente:** Dificultad para conocer tallas, colores y existencias disponibles en tiempo real.
3. **Ausencia de carrito de compras:** Imposibilidad de agrupar productos y calcular subtotales antes de decidir la compra.
4. **Falta de identidad digital propia:** Los negocios pequeños carecen de una tienda web estructurada.

**Solución (Slam Shoes):** Plataforma web desacoplada con catálogo estructurado, autenticación segura de usuarios (clientes y administradores), gestión de carrito de compras y registro de pedidos.

> **Alcance:** Prototipo escolar enfocado en demostrar una arquitectura web completa (FrontEnd, BackEnd y base de datos en la nube) y los flujos principales de comercio electrónico. Los pagos reales y la logística de envíos quedan fuera del alcance.

---

## 2. Tecnologías Utilizadas y Escalabilidad

### Enfoque de Escalabilidad y Elección Tecnológica (Backend Java & Spring Boot)
Para el desarrollo del servidor, se seleccionó **Java 17+ con Spring Boot**, una decisión fundamentada en la arquitectura de nivel empresarial. A diferencia de lenguajes de tipado dinámico, Spring Boot aporta:
* **Tipado estricto y compilación segura:** Detecta fallos estructurales antes de ejecutar el código, facilitando el mantenimiento a largo plazo.
* **Inyección de dependencias y separación de capas:** Controladores, servicios y repositorios perfectamente desacoplados.
* **Alta concurrencia:** Gestión eficiente de hilos (*multithreading*) para procesar miles de transacciones concurrentes.

**Comparativa con aplicaciones reales:** Este mismo estándar de robustez y arquitectura orientada a microservicios con Spring Boot y Java es utilizado por gigantes tecnológicos globales para soportar operaciones masivas y críticas, tales como **Mercado Libre** (procesamiento masivo de e-commerce), **Netflix** (streaming y pasarelas concurrentes), **LinkedIn** y **Uber** (gestión transaccional segura).

### Tabla de Tecnologías

| Componente | Tecnología / Herramienta | Descripción y Propósito |
| :--- | :--- | :--- |
| **Lenguaje de Programación (BackEnd)** | **Java 17+** <br> ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) <br> **Apache Tomcat (Embebido)** | Núcleo de la API REST encargado de la lógica de negocio, seguridad y gestión de peticiones concurrentes bajo un entorno robusto y escalable. |
| **Framework FrontEnd** | **Vue.js 3** <br> ![Vue.js](https://img.shields.io/badge/Vue.js-35495E?style=for-the-badge&logo=vue.js&logoColor=4FC08D) <br> **Pinia & Vite** | Desarrollo de una Single Page Application (SPA) responsiva y ultrarrápida, con gestión de estado global para el carrito y sesiones de usuario. |
| **Base de Datos & Cloud** | **PostgreSQL** <br> ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) <br> **Supabase** | Base de datos relacional alojada en la nube con integridad referencial estricta mediante llaves foráneas. |
| **Herramientas de Pruebas** | **Bruno API Client** <br> ![Bruno](https://img.shields.io/badge/Bruno-7B61FF?style=for-the-badge&logo=bruno&logoColor=white) | Validación independiente de los endpoints REST del backend antes de la integración con el frontend. |

---

## 3. Base de datos (Manejo de 6 Tablas en Supabase)
El modelo relacional se compone de las siguientes tablas interconectadas:
1. **`usuarios`**: Almacena clientes y administradores con contraseñas cifradas en hash.
2. **`categorias`**: Clasificación de productos.
3. **`productos`**: Catálogo general de modelos de calzado deportivo.
4. **`producto_tallas`**: Control de stock por cada combinación específica de talla y color (variantes).
5. **`pedidos`**: Encabezado de cada compra realizada por los usuarios autenticados.
6. **`pedido_detalle`**: Renglones detallados de los productos, variantes y cantidades de cada pedido.

---

## 4. Endpoints Principales de la API REST

| Método | Ruta | Descripción |
| :--- | :--- | :--- |
| **POST** | `/api/auth/registro` | Registra un usuario nuevo en el sistema. |
| **POST** | `/api/auth/login` | Valida credenciales (correo/contraseña) y devuelve datos y rol. |
| **GET** | `/api/productos` | Lista los productos con filtros opcionales. |
| **GET** | `/api/productos/{id}` | Muestra el detalle de un producto, incluyendo tallas y stock. |
| **POST** | `/api/pedidos` | Registra un nuevo pedido a partir del carrito de compras. |
| **GET** | `/api/pedidos/usuario/{id}` | Consulta el historial de pedidos de un usuario. |
| **POST/PUT/DELETE** | `/api/productos` | Alta, edición y baja de productos (exclusivo para administradores). |

---

## 5. Pasos de Integración del Proyecto

1. **Creación del proyecto** y configuración del esquema relacional en Supabase.
2. **Configuración del BackEnd:** Establecer la conexión a Supabase con variables de entorno y pruebas de endpoints mediante **Bruno API Client**.
3. **Desarrollo en Spring Boot:** Implementación de controladores, servicios y repositorios para usuarios y productos.
4. **Desarrollo en Vue.js:** Creación de vistas (inicio, login, registro, catálogo, detalle, carrito) y configuración de rutas.
5. **Gestión de Estado:** Creación de stores de Pinia para el carrito de compras y la sesión de usuario.
6. **Integración Local:** Configuración de **CORS** en Spring Boot (`http://localhost:8080`) para permitir la comunicación con el FrontEnd (`http://localhost:5173`).
7. **Despliegue y Producción:** Publicación de BackEnd y FrontEnd en **Render**, actualizando las políticas de CORS correspondientes.

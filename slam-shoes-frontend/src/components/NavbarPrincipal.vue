<template>
  <!-- Espacio que ocupa la barra (la barra es fija y se oculta al bajar / reaparece al subir) -->
  <div :style="{ height: alturaBarra + 'px' }" aria-hidden="true"></div>

  <!-- Encabezado (Navbar) compartido por todas las vistas -->
  <nav ref="barra" class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary px-4 barra-fija" :class="{ 'barra-oculta': oculta }">
    <div class="container-fluid px-0">
      <router-link class="navbar-brand" :to="rutaInicio">
        <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes Logo" style="height: 45px; width: auto;" class="object-contain">
      </router-link>

      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menuPrincipal" aria-controls="menuPrincipal" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>

      <div class="collapse navbar-collapse justify-content-center" id="menuPrincipal">
        <ul class="navbar-nav mb-2 mb-lg-0">
          <!-- Solo aparece fuera del inicio -->
          <li class="nav-item" v-if="!esInicio">
            <router-link class="nav-link text-light" :to="rutaInicio"><i class="bi bi-house me-1"></i>Volver a inicio</router-link>
          </li>
          <!-- Catálogo: categorías reales de la tabla "categorias" -->
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownCatalogo" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Catálogo
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownCatalogo">
              <li v-for="categoria in categorias" :key="categoria.idCategoria">
                <router-link class="dropdown-item" :to="`/categoria/${categoria.idCategoria}`">
                  {{ categoria.nombre }}
                </router-link>
              </li>
              <li v-if="categorias.length === 0">
                <span class="dropdown-item-text text-secondary small">
                  {{ cargandoCategorias ? 'Cargando categorías...' : 'Sin categorías registradas' }}
                </span>
              </li>
            </ul>
          </li>

          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownMarcas" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Marcas
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownMarcas">
              <li v-for="marca in marcas" :key="marca.idMarca">
                <router-link class="dropdown-item" :to="`/marca/${marca.idMarca}`">
                  {{ marca.nombre }}
                </router-link>
              </li>
              <li v-if="marcas.length === 0">
                <span class="dropdown-item-text text-secondary small">
                  {{ cargandoMarcas ? 'Cargando marcas...' : 'Sin marcas registradas' }}
                </span>
              </li>
            </ul>
          </li>

          <!-- Jugadores: solo los registrados en la tabla "jugadores" -->
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownPlayer" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Por Jugador
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownPlayer">
              <li v-for="jugador in jugadores" :key="jugador.idJugador">
                <router-link class="dropdown-item" :to="`/jugador/${jugador.idJugador}`">
                  {{ jugador.nombre }}
                </router-link>
              </li>
              <li v-if="jugadores.length === 0">
                <span class="dropdown-item-text text-secondary small">
                  {{ cargandoJugadores ? 'Cargando jugadores...' : 'Sin jugadores registrados' }}
                </span>
              </li>
            </ul>
          </li>

          <!-- Colecciones: solo las activas de la tabla "colecciones" -->
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownColecciones" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Colecciones
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownColecciones">
              <li v-for="coleccion in colecciones" :key="coleccion.idColeccion">
                <router-link class="dropdown-item" :to="`/coleccion/${coleccion.idColeccion}`">
                  {{ coleccion.nombre }}
                </router-link>
              </li>
              <li v-if="colecciones.length === 0">
                <span class="dropdown-item-text text-secondary small">
                  {{ cargandoColecciones ? 'Cargando colecciones...' : 'Sin colecciones registradas' }}
                </span>
              </li>
            </ul>
          </li>
          <li class="nav-item"><router-link class="nav-link text-light" to="/ofertas">Ofertas</router-link></li>
        </ul>
      </div>

      <!-- Iconos de la derecha -->
      <div class="d-flex align-items-center gap-3">
        <!-- Buscador: nombre, marca o modelo (sin importar mayúsculas, acentos ni espacios) -->
        <form class="d-flex align-items-center buscador-nav" :class="{ abierto: buscadorAbierto }" @submit.prevent="buscar" role="search">
          <input
            ref="campoBusqueda" v-model="textoBusqueda" type="search"
            class="form-control form-control-sm bg-black text-light border-secondary campo-busqueda"
            placeholder="Buscar nombre, marca o modelo..." aria-label="Buscar productos"
            @keydown.esc="buscadorAbierto = false"
          />
          <button type="button" class="btn btn-link text-light fs-5 p-0 ms-2" aria-label="Buscar" @click="alternarBuscador">
            <i class="bi bi-search"></i>
          </button>
        </form>

        <!-- Cliente: favoritos con contador -->
        <a v-if="!esModoAdmin" href="#" class="position-relative text-light text-decoration-none fs-5" @click.prevent="irAFavoritos" aria-label="Favoritos">
          <i class="bi bi-heart"></i>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
            {{ totalItemsFavoritos }}
          </span>
        </a>

        <!-- Icono de Usuario -->
        <a href="#" class="text-light fs-5 text-decoration-none" @click.prevent="manejarClickUsuario" aria-label="Perfil de usuario">
          <i class="bi bi-person"></i>
        </a>

        <!-- Administrador: edición (módulos) y pedidos con contador -->
        <router-link v-if="esModoAdmin" to="/admin/modulos" class="text-light fs-5 text-decoration-none" aria-label="Módulos de edición" title="Módulos de edición">
          <i class="bi bi-pencil-square"></i>
        </router-link>
        <router-link v-if="esModoAdmin" to="/admin/pedidos" class="position-relative text-light text-decoration-none fs-5" aria-label="Pedidos" title="Pedidos">
          <i class="bi bi-box-seam"></i>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
            {{ totalPedidos }}
          </span>
        </router-link>

        <!-- Botón de Carrito Desplegable (el offcanvas lo aporta CarritoComponent en cada vista) -->
        <a v-if="!esModoAdmin" href="#offcanvasCarrito" data-bs-toggle="offcanvas" class="position-relative text-light text-decoration-none fs-5" aria-label="Abrir carrito">
          <i class="bi bi-bag"></i>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
            {{ totalItemsCarrito }}
          </span>
        </a>
      </div>
    </div>
  </nav>
</template>

<script>
import { ref, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { verificarSesionYAccionar, esAdmin } from '@/utils/auth.js';
import { obtenerJugadores, obtenerColecciones, obtenerMarcas, obtenerCategorias } from '@/utils/catalogos.js';

export default {
  name: 'NavbarPrincipal',
  props: {
    // En modo administrador se ocultan favoritos y carrito y se muestran edición y pedidos
    modoAdmin: { type: Boolean, default: false }
  },
  setup(props) {
    const router = useRouter();
    const route = useRoute();
    // Se activa con la prop o automáticamente si quien inició sesión es el ADMIN
    const esModoAdmin = computed(() => props.modoAdmin || esAdmin());
    const rutaInicio = computed(() => (esModoAdmin.value ? '/admin' : '/'));
    const esInicio = computed(() => route.path === rutaInicio.value);
    const totalPedidos = ref(0);

    // ---------- Barra fija: se oculta al bajar y reaparece al subir ----------
    const barra = ref(null);
    const alturaBarra = ref(70);
    const oculta = ref(false);
    let ultimoScroll = 0;

    const medirBarra = () => { if (barra.value) alturaBarra.value = barra.value.offsetHeight; };
    const alScroll = () => {
      const y = window.scrollY;
      if (y <= 80) oculta.value = false;               // arriba del todo: siempre visible
      else if (y > ultimoScroll + 6) oculta.value = true;  // bajando: se oculta
      else if (y < ultimoScroll - 6) oculta.value = false; // subiendo: reaparece
      ultimoScroll = y;
    };

    // ---------- Buscador ----------
    const buscadorAbierto = ref(false);
    const textoBusqueda = ref(String(route.query.q || ''));
    const campoBusqueda = ref(null);

    const alternarBuscador = async () => {
      if (!buscadorAbierto.value) {
        buscadorAbierto.value = true;
        await nextTick();
        campoBusqueda.value?.focus();
      } else if (textoBusqueda.value.trim()) {
        buscar();
      } else {
        buscadorAbierto.value = false;
      }
    };
    const buscar = () => {
      const q = textoBusqueda.value.trim();
      if (!q) return;
      router.push({ path: '/buscar', query: { q } });
    };
    // Si ya se está en la página de resultados, el buscador se muestra abierto con lo buscado
    watch(() => route.fullPath, () => {
      if (route.path === '/buscar') { textoBusqueda.value = String(route.query.q || ''); buscadorAbierto.value = true; }
      else buscadorAbierto.value = false;
    });

    const jugadores = ref([]);
    const cargandoJugadores = ref(true);
    const colecciones = ref([]);
    const marcas = ref([]);
    const cargandoMarcas = ref(true);
    const categorias = ref([]);
    const cargandoCategorias = ref(true);
    const cargandoColecciones = ref(true);
    const favoritos = ref(JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || []);
    const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);

    const cargarJugadores = async () => {
      try {
        const respuesta = await obtenerJugadores();
        jugadores.value = respuesta;
      } catch (error) {
        console.error('Error al cargar los jugadores:', error);
      } finally {
        cargandoJugadores.value = false;
      }
    };

    const cargarColecciones = async () => {
      try {
        const respuesta = await obtenerColecciones();
        colecciones.value = respuesta;
      } catch (error) {
        console.error('Error al cargar las colecciones:', error);
      } finally {
        cargandoColecciones.value = false;
      }
    };

    const cargarMarcas = async () => {
      try {
        marcas.value = await obtenerMarcas();
      } catch (error) {
        console.error('Error al cargar las marcas:', error);
      } finally {
        cargandoMarcas.value = false;
      }
    };

    const cargarCategorias = async () => {
      try {
        categorias.value = await obtenerCategorias();
      } catch (error) {
        console.error('Error al cargar las categorías:', error);
      } finally {
        cargandoCategorias.value = false;
      }
    };

    const cargarTotalPedidos = async () => {
      try {
        totalPedidos.value = (await axios.get('http://localhost:8080/api/admin/pedidos/conteo')).data.total;
      } catch (error) {
        console.error('Error al cargar el contador de pedidos:', error);
      }
    };

    // Los contadores se vuelven a leer cuando cualquier vista dispara el evento 'storage'
    const refrescarContadores = () => {
      favoritos.value = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      carrito.value = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
    };

    const manejarClickUsuario = () => {
      const usuarioGuardado = localStorage.getItem('usuario');
      router.push(usuarioGuardado ? '/profile' : '/login');
    };

    const irAFavoritos = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;
      router.push('/favoritos');
    };

    const totalItemsFavoritos = computed(() => favoritos.value.length);
    const totalItemsCarrito = computed(() =>
      carrito.value.reduce((suma, item) => suma + item.cantidad, 0)
    );

    onMounted(() => {
      cargarJugadores();
      cargarColecciones();
      cargarMarcas();
      cargarCategorias();
      if (esModoAdmin.value) cargarTotalPedidos();
      window.addEventListener('storage', refrescarContadores);
      window.addEventListener('scroll', alScroll, { passive: true });
      window.addEventListener('resize', medirBarra);
      ultimoScroll = window.scrollY;
      medirBarra();
      if (route.path === '/buscar') buscadorAbierto.value = true;
    });

    onBeforeUnmount(() => {
      window.removeEventListener('storage', refrescarContadores);
      window.removeEventListener('scroll', alScroll);
      window.removeEventListener('resize', medirBarra);
    });

    return {
      barra,
      alturaBarra,
      oculta,
      buscadorAbierto,
      textoBusqueda,
      campoBusqueda,
      alternarBuscador,
      buscar,
      esModoAdmin,
      rutaInicio,
      esInicio,
      totalPedidos,
      jugadores,
      cargandoJugadores,
      colecciones,
      cargandoColecciones,
      marcas,
      cargandoMarcas,
      categorias,
      cargandoCategorias,
      manejarClickUsuario,
      irAFavoritos,
      totalItemsFavoritos,
      totalItemsCarrito
    };
  }
};
</script>

<style scoped>
/* Barra fija arriba: baja de nivel (se oculta) al desplazarse hacia abajo y vuelve al subir */
.barra-fija {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1030;
  transition: transform 0.3s ease;
}
.barra-oculta { transform: translateY(-100%); }

/* Campo de búsqueda: colapsado hasta pulsar la lupa */
.campo-busqueda {
  width: 0;
  padding-left: 0;
  padding-right: 0;
  border-width: 0 !important;
  opacity: 0;
  transition: width 0.25s ease, opacity 0.25s ease;
}
.buscador-nav.abierto .campo-busqueda {
  width: 260px;
  max-width: 45vw;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
  border-width: 1px !important;
  opacity: 1;
}
</style>

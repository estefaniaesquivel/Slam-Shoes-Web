import axios from 'axios';

const API_URL = 'http://localhost:8080/api/productos';

export default {
  // Obtener todos los productos para mostrarlos en el index
  obtenerProductos() {
    return axios.get(API_URL);
  }
};
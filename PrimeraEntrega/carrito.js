// Manejo del carrito de compras con persistencia local

const CLAVE_STORAGE = 'slam_shoes_cart';

function obtenerCarrito() {
    return JSON.parse(localStorage.getItem(CLAVE_STORAGE)) || [];
}

function guardarCarrito(carrito) {
    localStorage.setItem(CLAVE_STORAGE, JSON.stringify(carrito));
    renderizarCarrito();
}

function actualizarBadge() {
    const carrito = obtenerCarrito();
    const totalCantidad = carrito.reduce((acumulado, item) => acumulado + item.cantidad, 0);
    const badge = document.getElementById('contador-carrito');
    if (badge) {
        badge.textContent = totalCantidad;
    }
}

function renderizarCarrito() {
    actualizarBadge();
    const contenedorLista = document.getElementById('lista-carrito');
    const contenedorTotal = document.getElementById('total-carrito');

    if (!contenedorLista) return;

    const carrito = obtenerCarrito();

    if (carrito.length === 0) {
        contenedorLista.innerHTML = '<p class="text-center text-muted my-4">El carrito está vacío</p>';
        if (contenedorTotal) contenedorTotal.textContent = '$0.00 MXN';
        return;
    }

    let total = 0;
    contenedorLista.innerHTML = '';

    carrito.forEach((item, index) => {
        const subtotal = item.precio * item.cantidad;
        total += subtotal;

        const elemento = document.createElement('div');
        elemento.className = 'd-flex justify-content-between align-items-center mb-3 pb-2 border-bottom';
        elemento.innerHTML = `
            <div>
                <h6 class="mb-0">${item.nombre}</h6>
                <small class="text-muted">$${item.precio.toLocaleString('es-MX')} x ${item.cantidad}</small>
            </div>
            <div class="d-flex align-items-center gap-2">
                <span class="fw-bold">$${subtotal.toLocaleString('es-MX')}</span>
                <button class="btn btn-sm btn-outline-danger btn-eliminar-item" data-index="${index}">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
        contenedorLista.appendChild(elemento);
    });

    if (contenedorTotal) {
        contenedorTotal.textContent = `$${total.toLocaleString('es-MX')} MXN`;
    }

    // Quitar producto de la lista
    contenedorLista.querySelectorAll('.btn-eliminar-item').forEach(boton => {
        boton.addEventListener('click', (e) => {
            const index = e.currentTarget.dataset.index;
            eliminarItem(index);
        });
    });
}

function agregarAlCarrito(producto) {
    const carrito = obtenerCarrito();
    const itemExistente = carrito.find(item => item.id === producto.id);

    if (itemExistente) {
        itemExistente.cantidad += 1;
    } else {
        carrito.push({
            id: producto.id,
            nombre: producto.nombre,
            precio: producto.precio,
            cantidad: 1
        });
    }

    guardarCarrito(carrito);
}

function eliminarItem(index) {
    const carrito = obtenerCarrito();
    carrito.splice(index, 1);
    guardarCarrito(carrito);
}

function vaciarCarrito() {
    localStorage.removeItem(CLAVE_STORAGE);
    renderizarCarrito();
}

document.addEventListener('DOMContentLoaded', () => {
    renderizarCarrito();

    // Botones para agregar productos
    const botonesAgregar = document.querySelectorAll('.btn-agregar');
    botonesAgregar.forEach(boton => {
        boton.addEventListener('click', (e) => {
            const btn = e.currentTarget;
            const producto = {
                id: btn.dataset.id || 'prod-' + Math.random(),
                nombre: btn.dataset.nombre || 'Producto Slam Shoes',
                precio: parseFloat(btn.dataset.precio) || 1500
            };
            agregarAlCarrito(producto);
        });
    });

    // Boton para vaciar lista
    const btnVaciar = document.getElementById('btn-vaciar-carrito');
    if (btnVaciar) {
        btnVaciar.addEventListener('click', vaciarCarrito);
    }
});

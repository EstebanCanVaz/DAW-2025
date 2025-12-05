const API_BASE_URL = '/ProyectoPOO/api';

class APIClient {
    constructor() {
        this.baseURL = API_BASE_URL;
    }

    async get(endpoint, params = {}) {
        const url = new URL(this.baseURL + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));

        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (response.status === 401) {
                window.location.href = '/ProyectoPOO/login.html';
                return null;
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    async post(endpoint, data) {
        try {
            const response = await fetch(this.baseURL + endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(data)
            });

            if (response.status === 401) {
                window.location.href = '/ProyectoPOO/login.html';
                return null;
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    async put(endpoint, data) {
        try {
            const response = await fetch(this.baseURL + endpoint, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(data)
            });

            if (response.status === 401) {
                window.location.href = '/ProyectoPOO/login.html';
                return null;
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }

    async delete(endpoint, params = {}) {
        const url = new URL(this.baseURL + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));

        try {
            const response = await fetch(url, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (response.status === 401) {
                window.location.href = '/ProyectoPOO/login.html';
                return null;
            }

            return await response.json();
        } catch (error) {
            throw error;
        }
    }
}

const apiClient = new APIClient();

const ProductoAPI = {
    async listar() {
        return await apiClient.get('/productos');
    },

    async obtener(id) {
        return await apiClient.get('/productos', { id });
    },

    async buscar(busqueda) {
        return await apiClient.get('/productos', { busqueda });
    },

    async crear(producto) {
        return await apiClient.post('/productos', producto);
    },

    async actualizar(producto) {
        return await apiClient.put('/productos', producto);
    },

    async eliminar(id) {
        return await apiClient.delete('/productos', { id });
    }
};

const ClienteAPI = {
    async listar() {
        return await apiClient.get('/clientes');
    },

    async obtener(id) {
        return await apiClient.get('/clientes', { id });
    },

    async buscar(busqueda) {
        return await apiClient.get('/clientes', { busqueda });
    },

    async crear(cliente) {
        return await apiClient.post('/clientes', cliente);
    },

    async actualizar(cliente) {
        return await apiClient.put('/clientes', cliente);
    },

    async eliminar(id) {
        return await apiClient.delete('/clientes', { id });
    }
};

const VentaAPI = {
    async procesar(venta) {
        return await apiClient.post('/ventas', venta);
    }
};

const ReporteAPI = {
    async buscarFactura(idfactura) {
        return await apiClient.get('/reportes', { action: 'factura', id: idfactura });
    },

    async reportePorFechas(fechaInicio, fechaFin) {
        return await apiClient.get('/reportes', { 
            action: 'fechas', 
            fechaInicio, 
            fechaFin 
        });
    },

    async dashboardStats() {
        return await apiClient.get('/reportes', { action: 'dashboard' });
    }
};

const SessionAPI = {
    async verificar() {
        return await apiClient.get('/session');
    },

    async cerrarSesion() {
        try {
            const response = await fetch(API_BASE_URL + '/logout', {
                method: 'POST',
                credentials: 'same-origin'
            });
            const data = await response.json();
            if (data.success) {
                localStorage.removeItem('usuario');
                window.location.href = '/ProyectoPOO/login.html';
            }
            return data;
        } catch (error) {
            throw error;
        }
    }
};

window.addEventListener('load', async function() {
    if (!window.location.pathname.includes('login.html')) {
        try {
            const session = await SessionAPI.verificar();
            if (!session || !session.authenticated) {
                window.location.href = '/ProyectoPOO/login.html';
            } else if (session.usuario) {
                localStorage.setItem('usuario', JSON.stringify(session.usuario));
                actualizarInfoUsuario(session.usuario);
            }
        } catch (error) {
            window.location.href = '/ProyectoPOO/login.html';
        }
    }
});

function actualizarInfoUsuario(usuario) {
    const navbarBrand = document.querySelector('.navbar-brand h1');
    if (navbarBrand && usuario.nombre) {
        let userInfo = document.getElementById('userInfo');
        if (!userInfo) {
            userInfo = document.createElement('div');
            userInfo.id = 'userInfo';
            userInfo.style.cssText = 'color: white; font-size: 14px; margin-left: 15px;';
            navbarBrand.parentElement.appendChild(userInfo);
        }
        userInfo.innerHTML = `<i class="fas fa-user"></i> ${usuario.nombre} (${usuario.rol}) 
                              <a href="#" onclick="cerrarSesion()" style="color: #ff6b6b; margin-left: 10px;">
                              <i class="fas fa-sign-out-alt"></i> Salir</a>`;
    }
}

async function cerrarSesion() {
    if (confirm('¿Estás seguro de cerrar sesión?')) {
        await SessionAPI.cerrarSesion();
    }
}

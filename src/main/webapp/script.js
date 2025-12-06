// ============================================
// Sistema POS - JavaScript
// ============================================

// ============================================
// NOTA: Las funciones de Database ahora usan AJAX + JSON
// Los datos se obtienen del servidor a través de los servlets REST
// ============================================

// ============================================
// Gestión de Navegación
// ============================================
// Ya no es necesaria - Cada página es independiente

// ============================================
// Gestión de Dashboard
// ============================================
class DashboardManager {
  constructor() {
    if (this.checkElements()) {
      this.updateStats();
    }
  }

  checkElements() {
    return (
      document.getElementById("totalProductos") &&
      document.getElementById("totalClientes") &&
      document.getElementById("totalVentas") &&
      document.getElementById("ventasTotal")
    );
  }

  async updateStats() {
    if (!this.checkElements()) return;

    try {
      const response = await ReporteAPI.dashboardStats();

      if (response && response.success) {
        const stats = response.data;
        document.getElementById("totalProductos").textContent =
          stats.totalProductos;
        document.getElementById("totalClientes").textContent =
          stats.totalClientes;
        document.getElementById("totalVentas").textContent = stats.ventasHoy;
        document.getElementById("ventasTotal").textContent =
          "$" + stats.montoHoy.toFixed(2);
      }
    } catch (error) {
      showToast("Error al cargar estadísticas", "error");
    }
  }
}

// ============================================
// Gestión de Productos
// ============================================
class ProductoManager {
  constructor() {
    if (this.checkElements()) {
      this.initEventListeners();
      this.loadProductos();
    }
  }

  checkElements() {
    return (
      document.getElementById("btnNuevoProducto") &&
      document.getElementById("formProducto") &&
      document.getElementById("buscarProducto")
    );
  }

  initEventListeners() {
    document
      .getElementById("btnNuevoProducto")
      .addEventListener("click", () => {
        this.openModal();
      });

    document.getElementById("formProducto").addEventListener("submit", (e) => {
      e.preventDefault();
      this.saveProducto();
    });

    document.getElementById("buscarProducto").addEventListener("input", (e) => {
      this.searchProductos(e.target.value);
    });
  }

  async loadProductos(filter = "") {
    const tbody = document.getElementById("productosBody");
    if (!tbody) return;

    tbody.innerHTML =
      '<tr><td colspan="5" style="text-align:center;">Cargando...</td></tr>';

    try {
      let response;
      if (filter) {
        response = await ProductoAPI.buscar(filter);
      } else {
        response = await ProductoAPI.listar();
      }

      tbody.innerHTML = "";

      if (response && response.success && response.data) {
        response.data.forEach((producto) => {
          const tr = document.createElement("tr");

          // Badge de stock
          let stockBadge = "";
          if (producto.stock < 10) {
            stockBadge = `<span class="badge badge-danger">${producto.stock}</span>`;
          } else if (producto.stock < 20) {
            stockBadge = `<span class="badge badge-warning">${producto.stock}</span>`;
          } else {
            stockBadge = `<span class="badge badge-success">${producto.stock}</span>`;
          }

          tr.innerHTML = `
                        <td>${producto.idproducto}</td>
                        <td>${producto.nombre}</td>
                        <td>$${parseFloat(producto.precioProducto).toFixed(
                          2
                        )}</td>
                        <td>${stockBadge}</td>
                        <td class="table-actions">
                            <button class="btn btn-sm btn-info" onclick="productoManager.editProducto(${
                              producto.idproducto
                            })">
                                <i class="fas fa-edit"></i> Editar
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="productoManager.deleteProducto(${
                              producto.idproducto
                            })">
                                <i class="fas fa-trash"></i> Eliminar
                            </button>
                        </td>
                    `;
          tbody.appendChild(tr);
        });

        if (response.data.length === 0) {
          tbody.innerHTML =
            '<tr><td colspan="5" style="text-align:center;">No se encontraron productos</td></tr>';
        }
      } else {
        tbody.innerHTML =
          '<tr><td colspan="5" style="text-align:center;">Error al cargar productos</td></tr>';
      }
    } catch (error) {
      tbody.innerHTML =
        '<tr><td colspan="5" style="text-align:center;">Error al cargar productos</td></tr>';
      showToast("Error al cargar productos", "error");
    }
  }

  searchProductos(query) {
    this.loadProductos(query);
  }

  openModal(producto = null) {
    const modal = document.getElementById("modalProducto");
    const form = document.getElementById("formProducto");

    form.reset();

    if (producto) {
      document.getElementById("tituloModalProducto").textContent =
        "Editar Producto";
      document.getElementById("idProductoEdit").value = producto.idproducto;
      document.getElementById("nombreProducto").value = producto.nombre;
      document.getElementById("precioProducto").value = producto.precioProducto;
      document.getElementById("stockProducto").value = producto.stock;
    } else {
      document.getElementById("tituloModalProducto").textContent =
        "Nuevo Producto";
      document.getElementById("idProductoEdit").value = "";
    }

    modal.classList.add("active");
  }

  async editProducto(id) {
    try {
      const response = await ProductoAPI.obtener(id);
      if (response && response.success) {
        this.openModal(response.data);
      } else {
        showToast("Producto no encontrado", "error");
      }
    } catch (error) {
      showToast("Error al obtener producto", "error");
    }
  }

  async saveProducto() {
    const idProducto = document.getElementById("idProductoEdit").value;
    const producto = {
      idproducto: idProducto ? parseInt(idProducto) : null,
      nombre: document.getElementById("nombreProducto").value,
      precioProducto: parseFloat(
        document.getElementById("precioProducto").value
      ),
      stock: parseInt(document.getElementById("stockProducto").value),
    };

    try {
      let response;
      if (idProducto) {
        response = await ProductoAPI.actualizar(producto);
      } else {
        response = await ProductoAPI.crear(producto);
      }

      if (response && response.success) {
        this.loadProductos();
        this.closeModal();
        showToast(
          idProducto
            ? "Producto actualizado exitosamente"
            : "Producto creado exitosamente",
          "success"
        );
      } else {
        showToast(response.message || "Error al guardar producto", "error");
      }
    } catch (error) {
      showToast("Error al guardar producto", "error");
    }
  }

  async deleteProducto(id) {
    if (confirm("¿Está seguro de eliminar este producto?")) {
      try {
        const response = await ProductoAPI.eliminar(id);
        if (response && response.success) {
          this.loadProductos();
          showToast("Producto eliminado exitosamente", "success");
        } else {
          showToast(response.message || "Error al eliminar producto", "error");
        }
      } catch (error) {
        showToast("Error al eliminar producto", "error");
      }
    }
  }

  closeModal() {
    document.getElementById("modalProducto").classList.remove("active");
  }
}

// ============================================
// Gestión de Clientes
// ============================================
class ClienteManager {
  constructor() {
    if (this.checkElements()) {
      this.initEventListeners();
      this.loadClientes();
    }
  }

  checkElements() {
    return (
      document.getElementById("btnNuevoCliente") &&
      document.getElementById("formCliente") &&
      document.getElementById("buscarCliente")
    );
  }

  initEventListeners() {
    document.getElementById("btnNuevoCliente").addEventListener("click", () => {
      this.openModal();
    });

    document.getElementById("formCliente").addEventListener("submit", (e) => {
      e.preventDefault();
      this.saveCliente();
    });

    document.getElementById("buscarCliente").addEventListener("input", (e) => {
      this.searchClientes(e.target.value);
    });
  }

  async loadClientes(filter = "") {
    const tbody = document.getElementById("clientesBody");
    if (!tbody) return;

    tbody.innerHTML =
      '<tr><td colspan="5" style="text-align:center;">Cargando...</td></tr>';

    try {
      let response;
      if (filter) {
        response = await ClienteAPI.buscar(filter);
      } else {
        response = await ClienteAPI.listar();
      }

      tbody.innerHTML = "";

      if (response && response.success && response.data) {
        response.data.forEach((cliente) => {
          const tr = document.createElement("tr");
          tr.innerHTML = `
                        <td>${cliente.idcliente}</td>
                        <td>${cliente.nombre}</td>
                        <td>${cliente.appaterno}</td>
                        <td>${cliente.apmaterno}</td>
                        <td>
                            <button class="btn btn-sm btn-warning" onclick="verDocumentosCliente(${cliente.idcliente})">
                                <i class="fas fa-file-alt"></i> Ver
                            </button>
                        </td>
                        <td class="table-actions">
                            <button class="btn btn-sm btn-info" onclick="clienteManager.editCliente(${cliente.idcliente})">
                                <i class="fas fa-edit"></i> Editar
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="clienteManager.deleteCliente(${cliente.idcliente})">
                                <i class="fas fa-trash"></i> Eliminar
                            </button>
                        </td>
                    `;
          tbody.appendChild(tr);
        });

        if (response.data.length === 0) {
          tbody.innerHTML =
            '<tr><td colspan="5" style="text-align:center;">No se encontraron clientes</td></tr>';
        }
      }
    } catch (error) {
      showToast("Error al cargar clientes", "error");
    }
  }

  searchClientes(query) {
    this.loadClientes(query);
  }

  openModal(cliente = null) {
    const modal = document.getElementById("modalCliente");
    const form = document.getElementById("formCliente");

    form.reset();

    if (cliente) {
      document.getElementById("tituloModalCliente").textContent =
        "Editar Cliente";
      document.getElementById("idClienteEdit").value = cliente.idcliente;
      document.getElementById("nombreCliente").value = cliente.nombre;
      document.getElementById("appaternoCliente").value = cliente.appaterno;
      document.getElementById("apmaternoCliente").value = cliente.apmaterno;
    } else {
      document.getElementById("tituloModalCliente").textContent =
        "Nuevo Cliente";
      document.getElementById("idClienteEdit").value = "";
    }

    modal.classList.add("active");
  }

  async editCliente(id) {
    try {
      const response = await ClienteAPI.obtener(id);
      if (response && response.success) {
        this.openModal(response.data);
      } else {
        showToast("Cliente no encontrado", "error");
      }
    } catch (error) {
      showToast("Error al obtener cliente", "error");
    }
  }

  async saveCliente() {
    const idCliente = document.getElementById("idClienteEdit").value;
    const cliente = {
      idcliente: idCliente ? parseInt(idCliente) : null,
      nombre: document.getElementById("nombreCliente").value,
      appaterno: document.getElementById("appaternoCliente").value,
      apmaterno: document.getElementById("apmaternoCliente").value,
    };

    try {
      let response;
      if (idCliente) {
        response = await ClienteAPI.actualizar(cliente);
      } else {
        response = await ClienteAPI.crear(cliente);
      }

      if (response && response.success) {
        this.loadClientes();
        this.closeModal();
        showToast(
          idCliente
            ? "Cliente actualizado exitosamente"
            : "Cliente creado exitosamente",
          "success"
        );
      } else {
        showToast(response.message || "Error al guardar cliente", "error");
      }
    } catch (error) {
      showToast("Error al guardar cliente", "error");
    }
  }

  async deleteCliente(id) {
    if (confirm("¿Está seguro de eliminar este cliente?")) {
      try {
        const response = await ClienteAPI.eliminar(id);
        if (response && response.success) {
          this.loadClientes();
          showToast("Cliente eliminado exitosamente", "success");
        } else {
          showToast(response.message || "Error al eliminar cliente", "error");
        }
      } catch (error) {
        showToast("Error al eliminar cliente", "error");
      }
    }
  }

  closeModal() {
    document.getElementById("modalCliente").classList.remove("active");
  }
}

// ============================================
// Gestión de Ventas
// ============================================
class VentaManager {
  constructor() {
    this.carrito = [];
    if (this.checkElements()) {
      this.initEventListeners();
      this.initVenta();
    }
  }

  checkElements() {
    return (
      document.getElementById("selectProducto") &&
      document.getElementById("btnAgregarProducto") &&
      document.getElementById("btnFinalizarVenta")
    );
  }

  initEventListeners() {
    document
      .getElementById("selectProducto")
      .addEventListener("change", (e) => {
        this.onProductoChange(e.target.value);
      });

    document
      .getElementById("btnAgregarProducto")
      .addEventListener("click", () => {
        this.agregarAlCarrito();
      });

    document
      .getElementById("btnFinalizarVenta")
      .addEventListener("click", () => {
        this.finalizarVenta();
      });
  }

  initVenta() {
    this.carrito = [];
    this.loadSelectClientes();
    this.loadSelectProductos();
    this.updateCarrito();
  }

  async loadSelectClientes() {
    const select = document.getElementById("selectCliente");
    if (!select) return;

    select.innerHTML = '<option value="">Cargando...</option>';

    try {
      const response = await ClienteAPI.listar();
      select.innerHTML = '<option value="">Seleccione un cliente...</option>';

      if (response && response.success && response.data) {
        response.data.forEach((cliente) => {
          const option = document.createElement("option");
          option.value = cliente.idcliente;
          option.textContent = `${cliente.nombre} ${cliente.appaterno} ${cliente.apmaterno}`;
          select.appendChild(option);
        });
      }
    } catch (error) {
      select.innerHTML = '<option value="">Error al cargar clientes</option>';
    }
  }

  async loadSelectProductos() {
    const select = document.getElementById("selectProducto");
    if (!select) return;

    select.innerHTML = '<option value="">Cargando...</option>';

    try {
      const response = await ProductoAPI.listar();
      select.innerHTML = '<option value="">Seleccione un producto...</option>';

      if (response && response.success && response.data) {
        // Guardar productos en cache para acceso rápido
        this.productosCache = response.data;

        this.updateProductosSelect();
      }
    } catch (error) {
      select.innerHTML = '<option value="">Error al cargar productos</option>';
    }
  }

  updateProductosSelect() {
    const select = document.getElementById("selectProducto");
    if (!select || !this.productosCache) return;

    // Guardar el producto seleccionado actualmente
    const selectedValue = select.value;

    // Limpiar opciones excepto la primera
    select.innerHTML = '<option value="">Seleccione un producto...</option>';

    // Calcular stock disponible considerando el carrito
    this.productosCache
      .filter((p) => {
        const cantidadEnCarrito = this.getCantidadEnCarrito(p.idproducto);
        return (p.stock - cantidadEnCarrito) > 0;
      })
      .forEach((producto) => {
        const cantidadEnCarrito = this.getCantidadEnCarrito(producto.idproducto);
        const stockDisponible = producto.stock - cantidadEnCarrito;
        
        const option = document.createElement("option");
        option.value = producto.idproducto;
        option.textContent = `${producto.nombre} - $${producto.precioProducto} (Stock: ${stockDisponible})`;
        select.appendChild(option);
      });

    // Restaurar selección si el producto aún está disponible
    if (selectedValue) {
      select.value = selectedValue;
    }
  }

  getCantidadEnCarrito(idproducto) {
    const item = this.carrito.find((i) => i.idproducto === parseInt(idproducto));
    return item ? item.cantidad : 0;
  }

  onProductoChange(idproducto) {
    if (idproducto && this.productosCache) {
      const producto = this.productosCache.find(
        (p) => p.idproducto === parseInt(idproducto)
      );
      if (producto) {
        const cantidadEnCarrito = this.getCantidadEnCarrito(producto.idproducto);
        const stockDisponible = producto.stock - cantidadEnCarrito;
        
        document.getElementById("precioVenta").value = producto.precioProducto;
        document.getElementById("cantidadProducto").max = stockDisponible;
      }
    } else {
      document.getElementById("precioVenta").value = "";
      document.getElementById("cantidadProducto").max = "";
    }
  }

  agregarAlCarrito() {
    const idproducto = document.getElementById("selectProducto").value;
    const cantidad = parseInt(
      document.getElementById("cantidadProducto").value
    );
    const precioVenta = parseFloat(
      document.getElementById("precioVenta").value
    );

    if (!idproducto) {
      showToast("Seleccione un producto", "warning");
      return;
    }

    if (!cantidad || cantidad <= 0) {
      showToast("Ingrese una cantidad válida", "warning");
      return;
    }

    const producto = this.productosCache.find(
      (p) => p.idproducto === parseInt(idproducto)
    );

    if (!producto) {
      showToast("Producto no encontrado", "error");
      return;
    }

    if (cantidad > producto.stock) {
      showToast("Cantidad no disponible en stock", "error");
      return;
    }

    // Verificar si el producto ya está en el carrito
    const existente = this.carrito.find(
      (item) => item.idproducto === parseInt(idproducto)
    );

    if (existente) {
      if (existente.cantidad + cantidad > producto.stock) {
        showToast("Cantidad total excede el stock disponible", "error");
        return;
      }
      existente.cantidad += cantidad;
    } else {
      this.carrito.push({
        idproducto: parseInt(idproducto),
        nombre: producto.nombre,
        cantidad: cantidad,
        precioVenta: precioVenta,
      });
    }

    this.updateCarrito();
    this.updateProductosSelect(); // Actualizar stock disponible
    showToast("Producto agregado al carrito", "success");

    // Resetear formulario
    document.getElementById("selectProducto").value = "";
    document.getElementById("cantidadProducto").value = 1;
    document.getElementById("precioVenta").value = "";
  }

  removeFromCarrito(index) {
    this.carrito.splice(index, 1);
    this.updateCarrito();
    this.updateProductosSelect(); // Actualizar stock disponible
    showToast("Producto eliminado del carrito", "info");
  }

  updateCarrito() {
    const tbody = document.getElementById("carritoBody");
    tbody.innerHTML = "";

    let total = 0;

    this.carrito.forEach((item, index) => {
      const subtotal = item.cantidad * item.precioVenta;
      total += subtotal;

      const tr = document.createElement("tr");
      tr.innerHTML = `
                <td>${item.nombre}</td>
                <td>${item.cantidad}</td>
                <td>$${item.precioVenta.toFixed(2)}</td>
                <td>$${subtotal.toFixed(2)}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="ventaManager.removeFromCarrito(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                </td>
            `;
      tbody.appendChild(tr);
    });

    document.getElementById("totalVenta").textContent = total.toFixed(2);
  }

  async finalizarVenta() {
    console.log("Ejecutando finalizarVenta:", Date.now());
    const idcliente = document.getElementById("selectCliente").value;

    if (!idcliente) {
      showToast("Seleccione un cliente", "warning");
      return;
    }

    if (this.carrito.length === 0) {
      showToast("Agregue productos al carrito", "warning");
      return;
    }

    // Preparar datos de la venta
    const venta = {
      idcliente: parseInt(idcliente),
      token: Date.now(),
      detalles: this.carrito.map((item) => ({
        idproducto: item.idproducto,
        cantidad: item.cantidad,
        precioVenta: item.precioVenta,
      })),
    };

    try {
      const response = await VentaAPI.procesar(venta);

      if (response && response.success) {
        showToast(
          `Venta finalizada. Factura #${
            response.idfactura
          } - Total: $${response.total.toFixed(2)}`,
          "success"
        );

        // Limpiar carrito y reiniciar
        this.carrito = [];
        this.initVenta();

        // Actualizar estadísticas del dashboard si existe
        if (dashboardManager && dashboardManager.updateStats) {
          dashboardManager.updateStats();
        }
      } else {
        showToast(response.message || "Error al procesar venta", "error");
      }
    } catch (error) {
      showToast("Error al procesar venta", "error");
    }
  }
}

// ============================================
// Gestión de Reportes
// ============================================
class ReporteManager {
  constructor() {
    if (this.checkElements()) {
      this.initEventListeners();
    }
  }

  checkElements() {
    return (
      document.getElementById("btnBuscarFactura") &&
      document.getElementById("btnGenerarReporte")
    );
  }

  initEventListeners() {
    document
      .getElementById("btnBuscarFactura")
      .addEventListener("click", () => {
        this.buscarFactura();
      });

    document
      .getElementById("btnGenerarReporte")
      .addEventListener("click", () => {
        this.generarReporte();
      });
  }

  async buscarFactura() {
    const idfactura = document.getElementById("buscarFactura").value;

    if (!idfactura) {
      showToast("Ingrese un número de factura", "warning");
      return;
    }

    try {
      const response = await ReporteAPI.buscarFactura(idfactura);

      if (response && response.success) {
        const factura = response.data;

        let detallesHTML =
          '<table style="width: 100%; margin-top: 1rem;"><thead><tr><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Subtotal</th></tr></thead><tbody>';

        factura.detalles.forEach((detalle) => {
          detallesHTML += `
                        <tr>
                            <td>${detalle.producto}</td>
                            <td>${detalle.cantidad}</td>
                            <td>$${detalle.precioVenta.toFixed(2)}</td>
                            <td>$${detalle.subtotal.toFixed(2)}</td>
                        </tr>
                    `;
        });

        detallesHTML += `</tbody></table>`;

        const detalleDiv = document.getElementById("detalleFactura");
        const qrHTML =
          typeof qrCodeAPI !== "undefined"
            ? generarQRParaFactura({
                idfactura: factura.idfactura,
                fecha: factura.fechaFactura,
                cliente: factura.cliente,
                total: factura.total,
              })
            : "";

        detalleDiv.innerHTML = `
                    <h3>Factura #${factura.idfactura}</h3>
                    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 2rem; margin-bottom: 1rem;">
                        <div>
                    <p><strong>Fecha:</strong> ${factura.fechaFactura}</p>
                    <p><strong>Cliente:</strong> ${factura.cliente}</p>
                    ${detallesHTML}
                    <div style="text-align: right; margin-top: 1rem; font-size: 1.5rem; font-weight: bold;">
                        Total: $${factura.total.toFixed(2)}
                            </div>
                        </div>
                        <div>
                            ${qrHTML}
                        </div>
                    </div>
                `;

        detalleDiv.style.display = "block";
        document.getElementById("resultadosReporte").style.display = "none";
      } else {
        showToast(response.message || "Factura no encontrada", "error");
      }
    } catch (error) {
      showToast("Error al buscar factura", "error");
    }
  }

  async generarReporte() {
    const fechaInicio = document.getElementById("fechaInicio").value;
    const fechaFin = document.getElementById("fechaFin").value;

    if (!fechaInicio || !fechaFin) {
      showToast("Seleccione ambas fechas", "warning");
      return;
    }

    if (fechaInicio > fechaFin) {
      showToast("La fecha de inicio debe ser menor a la fecha fin", "error");
      return;
    }

    try {
      const response = await ReporteAPI.reportePorFechas(fechaInicio, fechaFin);

      if (response && response.success) {
        const tbody = document.getElementById("reporteBody");
        tbody.innerHTML = "";

        if (response.data.length === 0) {
          showToast("No se encontraron ventas en el rango de fechas", "info");
          tbody.innerHTML =
            '<tr><td colspan="7" style="text-align:center;">No se encontraron resultados</td></tr>';
          return;
        }

        response.data.forEach((venta) => {
          const tr = document.createElement("tr");
          tr.innerHTML = `
                        <td>${venta.idfactura}</td>
                        <td>${venta.fechaFactura}</td>
                        <td>${venta.cliente}</td>
                        <td>${venta.producto}</td>
                        <td>${venta.cantidad}</td>
                        <td>$${venta.precioVenta.toFixed(2)}</td>
                        <td>$${venta.total.toFixed(2)}</td>
                    `;
          tbody.appendChild(tr);
        });

        // Agregar fila de total
        const trTotal = document.createElement("tr");
        trTotal.style.fontWeight = "bold";
        trTotal.style.backgroundColor = "#f8fafc";
        trTotal.innerHTML = `
                    <td colspan="6" style="text-align: right;">TOTAL GENERAL:</td>
                    <td>$${response.totalGeneral.toFixed(2)}</td>
                `;
        tbody.appendChild(trTotal);

        document.getElementById("resultadosReporte").style.display = "block";
        document.getElementById("detalleFactura").style.display = "none";

        showToast(
          `Reporte generado: ${response.totalVentas} ventas encontradas`,
          "success"
        );
      } else {
        showToast(response.message || "Error al generar reporte", "error");
      }
    } catch (error) {
      showToast("Error al generar reporte", "error");
    }
  }
}

// ============================================
// Utilidades
// ============================================
function showToast(message, type = "info") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = `toast ${type} show`;

  setTimeout(() => {
    toast.classList.remove("show");
  }, 3000);
}

// Cerrar modales
document.querySelectorAll(".close, .btn-secondary").forEach((element) => {
  element.addEventListener("click", (e) => {
    const modalId = element.dataset.modal || element.closest(".modal").id;
    if (modalId) {
      document.getElementById(modalId).classList.remove("active");
    }
  });
});

// Cerrar modal al hacer click fuera
window.addEventListener("click", (e) => {
  if (e.target.classList.contains("modal")) {
    e.target.classList.remove("active");
  }
});

// ============================================
// Inicialización de la Aplicación
// ============================================
const dashboardManager = new DashboardManager();
const productoManager = new ProductoManager();
const clienteManager = new ClienteManager();
const ventaManager = new VentaManager();
const reporteManager = new ReporteManager();

// ============================================
// [NUEVO] Lógica del Menú Responsive
// ============================================
document.addEventListener('DOMContentLoaded', () => {
    const menuToggle = document.getElementById('mobile-menu');
    const navbarMenu = document.querySelector('.navbar-menu');

    if (menuToggle && navbarMenu) {
        menuToggle.addEventListener('click', () => {
            navbarMenu.classList.toggle('active');
            
            // Cambiar ícono de barras a X
            const icon = menuToggle.querySelector('i');
            if (navbarMenu.classList.contains('active')) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            } else {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        });
    }
});

// Función Utilidad Toast (si no la tenías definida globalmente)
function showToast(message, type = "info") {
    const toast = document.getElementById("toast");
    if(toast) {
        toast.textContent = message;
        toast.className = `toast ${type} show`;
        setTimeout(() => { toast.classList.remove("show"); }, 3000);
    }
}
async function verDocumentosCliente(idcliente) {
    document.getElementById('idClienteDocumentos').value = idcliente;
    limpiarSeleccionArchivo();
    await cargarDocumentosCliente(idcliente);
    inicializarDragAndDrop();
    document.getElementById('modalDocumentos').classList.add('active');
}

function inicializarDragAndDrop() {
    const dropZone = document.getElementById('dropZone');
    if (!dropZone) return;
    
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, preventDefaults, false);
        document.body.addEventListener(eventName, preventDefaults, false);
    });
    
    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, highlight, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, unhighlight, false);
    });
    
    dropZone.addEventListener('drop', handleDrop, false);
}

function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

function highlight(e) {
    document.getElementById('dropZone').classList.add('drag-over');
}

function unhighlight(e) {
    document.getElementById('dropZone').classList.remove('drag-over');
}

function handleDrop(e) {
    const dt = e.dataTransfer;
    const files = dt.files;
    
    if (files.length > 0) {
        const fileInput = document.getElementById('archivoDocumento');
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(files[0]);
        fileInput.files = dataTransfer.files;
        
        mostrarArchivoSeleccionado(files[0]);
    }
}

function mostrarArchivoSeleccionado(file) {
    const fileName = document.getElementById('fileName');
    const btnCancelar = document.getElementById('btnCancelarArchivo');
    const btnSubir = document.getElementById('btnSubirDocumento');
    
    fileName.innerHTML = `
        <i class="fas fa-file-alt"></i> 
        <span style="margin: 0 10px;">${file.name}</span>
        <span style="color: #64748b; font-size: 0.85rem;">(${formatFileSize(file.size)})</span>
    `;
    fileName.style.display = 'flex';
    fileName.style.alignItems = 'center';
    fileName.style.justifyContent = 'center';
    fileName.style.gap = '10px';
    
    if (btnCancelar) btnCancelar.style.display = 'inline-block';
    if (btnSubir) btnSubir.disabled = false;
}

function limpiarSeleccionArchivo() {
    const fileInput = document.getElementById('archivoDocumento');
    const fileName = document.getElementById('fileName');
    const btnCancelar = document.getElementById('btnCancelarArchivo');
    const btnSubir = document.getElementById('btnSubirDocumento');
    
    if (fileInput) fileInput.value = '';
    if (fileName) {
        fileName.style.display = 'none';
        fileName.innerHTML = '';
    }
    if (btnCancelar) btnCancelar.style.display = 'none';
    if (btnSubir) btnSubir.disabled = true;
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('archivoDocumento');
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            if (this.files.length > 0) {
                mostrarArchivoSeleccionado(this.files[0]);
            }
        });
    }
    
    const btnCancelar = document.getElementById('btnCancelarArchivo');
    if (btnCancelar) {
        btnCancelar.addEventListener('click', limpiarSeleccionArchivo);
    }
});

async function cargarDocumentosCliente(idcliente) {
    const tbody = document.getElementById('documentosBody');
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Cargando...</td></tr>';
    
    try {
        const response = await fetch(`/ProyectoPOO/api/documentos?idcliente=${idcliente}`, {
            method: 'GET',
            credentials: 'same-origin'
        });
        
        const data = await response.json();
        tbody.innerHTML = '';
        
        if (data.success && data.data && data.data.length > 0) {
            data.data.forEach(doc => {
                const tr = document.createElement('tr');
                
                // Crear celdas de información
                const tdNombre = document.createElement('td');
                tdNombre.textContent = doc.nombre_archivo;
                
                const tdTipo = document.createElement('td');
                tdTipo.textContent = doc.tipo_archivo || 'N/A';
                
                const tdFecha = document.createElement('td');
                tdFecha.textContent = new Date(doc.fecha_subida).toLocaleString('es-MX');
                
                // Crear celda de acciones
                const tdAcciones = document.createElement('td');
                tdAcciones.className = 'table-actions';
                
                // Botón descargar
                const btnDescargar = document.createElement('button');
                btnDescargar.className = 'btn btn-sm btn-info';
                btnDescargar.innerHTML = '<i class="fas fa-download"></i> Descargar';
                btnDescargar.onclick = () => descargarDocumento(doc.iddocumento, doc.nombre_archivo);
                
                // Botón eliminar
                const btnEliminar = document.createElement('button');
                btnEliminar.className = 'btn btn-sm btn-danger';
                btnEliminar.innerHTML = '<i class="fas fa-trash"></i>';
                btnEliminar.onclick = () => eliminarDocumento(doc.iddocumento, idcliente);
                
                tdAcciones.appendChild(btnDescargar);
                tdAcciones.appendChild(btnEliminar);
                
                tr.appendChild(tdNombre);
                tr.appendChild(tdTipo);
                tr.appendChild(tdFecha);
                tr.appendChild(tdAcciones);
                
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No hay documentos</td></tr>';
        }
    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Error al cargar documentos</td></tr>';
        showToast('Error al cargar documentos', 'error');
    }
}

async function subirDocumento() {
    const idcliente = document.getElementById('idClienteDocumentos').value;
    const fileInput = document.getElementById('archivoDocumento');
    
    if (!fileInput.files || fileInput.files.length === 0) {
        showToast('Seleccione un archivo', 'warning');
        return;
    }
    
    const file = fileInput.files[0];
    
    if (file.size > 10 * 1024 * 1024) {
        showToast('El archivo no debe superar los 10MB', 'error');
        return;
    }
    
    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'application/msword', 
                         'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowedTypes.includes(file.type)) {
        showToast('Tipo de archivo no permitido. Use PDF, DOC, DOCX, JPG o PNG', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('idcliente', idcliente);
    formData.append('archivo', file);
    
    const btnSubir = document.getElementById('btnSubirDocumento');
    const originalText = btnSubir.innerHTML;
    
    try {
        btnSubir.disabled = true;
        btnSubir.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Subiendo...';
        
        const response = await fetch('/ProyectoPOO/api/documentos', {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Documento subido exitosamente', 'success');
            limpiarSeleccionArchivo();
            await cargarDocumentosCliente(idcliente);
        } else {
            showToast(data.message || 'Error al subir documento', 'error');
            btnSubir.disabled = false;
        }
    } catch (error) {
        showToast('Error al subir documento', 'error');
        btnSubir.disabled = false;
    } finally {
        btnSubir.innerHTML = originalText;
    }
}

async function descargarDocumento(iddocumento, nombreArchivo) {
    try {
        const response = await fetch(`/ProyectoPOO/api/documentos/download?id=${iddocumento}`, {
            method: 'GET',
            credentials: 'same-origin'
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = nombreArchivo;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            showToast('Documento descargado', 'success');
        } else {
            showToast('Error al descargar documento', 'error');
        }
    } catch (error) {
        showToast('Error al descargar documento', 'error');
    }
}

async function eliminarDocumento(iddocumento, idcliente) {
    if (!confirm('¿Está seguro de eliminar este documento?')) return;
    
    try {
        const response = await fetch(`/ProyectoPOO/api/documentos?id=${iddocumento}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showToast('Documento eliminado', 'success');
            await cargarDocumentosCliente(idcliente);
        } else {
            showToast(data.message || 'Error al eliminar documento', 'error');
        }
    } catch (error) {
        showToast('Error al eliminar documento', 'error');
    }
}


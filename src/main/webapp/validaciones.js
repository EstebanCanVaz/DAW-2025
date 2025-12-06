function validarTexto(valor, nombreCampo, minLength = 2, maxLength = 100) {
    if (!valor || valor.trim().length === 0) {
        return { valido: false, mensaje: `${nombreCampo} es requerido` };
    }
    if (valor.trim().length < minLength) {
        return { valido: false, mensaje: `${nombreCampo} debe tener al menos ${minLength} caracteres` };
    }
    if (valor.length > maxLength) {
        return { valido: false, mensaje: `${nombreCampo} no debe exceder ${maxLength} caracteres` };
    }
    const caracteresInvalidos = /<|>|&lt;|&gt;|<script|javascript:/gi;
    if (caracteresInvalidos.test(valor)) {
        return { valido: false, mensaje: `${nombreCampo} contiene caracteres no permitidos` };
    }
    return { valido: true };
}

function validarNumero(valor, nombreCampo, min = 0, max = Number.MAX_SAFE_INTEGER, entero = false) {
    if (valor === null || valor === undefined || valor === '') {
        return { valido: false, mensaje: `${nombreCampo} es requerido` };
    }
    const numero = parseFloat(valor);
    if (isNaN(numero)) {
        return { valido: false, mensaje: `${nombreCampo} debe ser un número válido` };
    }
    if (entero && !Number.isInteger(numero)) {
        return { valido: false, mensaje: `${nombreCampo} debe ser un número entero` };
    }
    if (numero < min) {
        return { valido: false, mensaje: `${nombreCampo} debe ser mayor o igual a ${min}` };
    }
    if (numero > max) {
        return { valido: false, mensaje: `${nombreCampo} no debe exceder ${max}` };
    }
    return { valido: true };
}

function validarPrecio(valor, nombreCampo = 'Precio') {
    const resultado = validarNumero(valor, nombreCampo, 0.01, 99999.99);
    if (!resultado.valido) return resultado;
    
    const precio = parseFloat(valor);
    const decimales = (precio.toString().split('.')[1] || '').length;
    if (decimales > 2) {
        return { valido: false, mensaje: `${nombreCampo} solo puede tener hasta 2 decimales` };
    }
    return { valido: true };
}

function validarStock(valor, nombreCampo = 'Stock') {
    return validarNumero(valor, nombreCampo, 0, 999999, true);
}

function validarFecha(valor, nombreCampo = 'Fecha') {
    if (!valor || valor.trim().length === 0) {
        return { valido: false, mensaje: `${nombreCampo} es requerida` };
    }
    const fecha = new Date(valor);
    if (isNaN(fecha.getTime())) {
        return { valido: false, mensaje: `${nombreCampo} no es válida` };
    }
    return { valido: true };
}

function validarRangoFechas(fechaInicio, fechaFin) {
    const resultadoInicio = validarFecha(fechaInicio, 'Fecha de inicio');
    if (!resultadoInicio.valido) return resultadoInicio;
    
    const resultadoFin = validarFecha(fechaFin, 'Fecha de fin');
    if (!resultadoFin.valido) return resultadoFin;
    
    const inicio = new Date(fechaInicio);
    const fin = new Date(fechaFin);
    
    if (inicio > fin) {
        return { valido: false, mensaje: 'La fecha de inicio debe ser anterior a la fecha de fin' };
    }
    const diffDays = Math.ceil((fin - inicio) / (1000 * 60 * 60 * 24));
    if (diffDays > 365) {
        return { valido: false, mensaje: 'El rango de fechas no debe exceder 1 año' };
    }
    return { valido: true };
}

function sanitizarEntrada(valor) {
    if (typeof valor !== 'string') return valor;
    
    let sanitizado = valor.trim();
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#x27;',
        "/": '&#x2F;'
    };
    return sanitizado.replace(/[&<>"'/]/g, char => map[char]);
}

function agregarValidacionTiempoReal(inputId, validador) {
    const input = document.getElementById(inputId);
    if (!input) return;
    
    input.addEventListener('blur', function() {
        const resultado = validador(this.value);
        if (!resultado.valido) {
            this.classList.add('is-invalid');
            this.classList.remove('is-valid');
            let errorDiv = this.nextElementSibling;
            if (!errorDiv || !errorDiv.classList.contains('invalid-feedback')) {
                errorDiv = document.createElement('div');
                errorDiv.className = 'invalid-feedback';
                this.parentNode.insertBefore(errorDiv, this.nextSibling);
            }
            errorDiv.textContent = resultado.mensaje;
            errorDiv.style.display = 'block';
            errorDiv.style.color = 'var(--danger-color)';
            errorDiv.style.fontSize = '0.875rem';
            errorDiv.style.marginTop = '0.25rem';
        } else {
            this.classList.remove('is-invalid');
            this.classList.add('is-valid');
            const errorDiv = this.nextElementSibling;
            if (errorDiv && errorDiv.classList.contains('invalid-feedback')) {
                errorDiv.style.display = 'none';
            }
        }
    });
    
    input.addEventListener('input', function() {
        this.classList.remove('is-invalid', 'is-valid');
        const errorDiv = this.nextElementSibling;
        if (errorDiv && errorDiv.classList.contains('invalid-feedback')) {
            errorDiv.style.display = 'none';
        }
    });
}

function inicializarValidaciones() {
    if (document.getElementById('nombreProducto')) {
        agregarValidacionTiempoReal('nombreProducto', (valor) => validarTexto(valor, 'Nombre del producto'));
        agregarValidacionTiempoReal('precioProducto', (valor) => validarPrecio(valor));
        agregarValidacionTiempoReal('stockProducto', (valor) => validarStock(valor));
    }
    
    if (document.getElementById('nombreCliente')) {
        agregarValidacionTiempoReal('nombreCliente', (valor) => validarTexto(valor, 'Nombre'));
        agregarValidacionTiempoReal('appaternoCliente', (valor) => validarTexto(valor, 'Apellido Paterno'));
        agregarValidacionTiempoReal('apmaternoCliente', (valor) => validarTexto(valor, 'Apellido Materno'));
    }
    
    if (document.getElementById('cantidadProducto')) {
        agregarValidacionTiempoReal('cantidadProducto', (valor) => validarNumero(valor, 'Cantidad', 1, 1000, true));
        agregarValidacionTiempoReal('precioVenta', (valor) => validarPrecio(valor, 'Precio de venta'));
    }
    
    if (document.getElementById('fechaInicio')) {
        agregarValidacionTiempoReal('fechaInicio', (valor) => validarFecha(valor, 'Fecha de inicio'));
        agregarValidacionTiempoReal('fechaFin', (valor) => validarFecha(valor, 'Fecha de fin'));
    }
}

document.addEventListener('DOMContentLoaded', inicializarValidaciones);

document.addEventListener('DOMContentLoaded', function() {
    const inputs = document.querySelectorAll('input[type="text"], input[type="number"], textarea');
    inputs.forEach(input => {
        input.addEventListener('input', function() {
            const scriptPattern = /<script|javascript:|onerror=|onload=/gi;
            if (scriptPattern.test(this.value)) {
                this.value = this.value.replace(scriptPattern, '');
                showToast('Caracteres no permitidos detectados y removidos', 'warning');
            }
        });
    });
});


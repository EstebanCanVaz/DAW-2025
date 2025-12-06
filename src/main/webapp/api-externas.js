class ExchangeRateAPI {
    constructor() {
        this.baseURL = 'https://api.exchangerate-api.com/v4/latest/USD';
    }

    async obtenerTipoCambio() {
        try {
            const response = await fetch(this.baseURL);
            const data = await response.json();
            
            if (data && data.rates && data.rates.MXN) {
                return {
                    success: true,
                    usd_to_mxn: data.rates.MXN,
                    fecha: data.date,
                    base: data.base
                };
            }
            return { success: false, message: 'No se pudo obtener el tipo de cambio' };
        } catch (error) {
            return { success: false, message: error.message };
        }
    }

    convertirMXNaUSD(montoMXN, tipoCambio) {
        return (montoMXN / tipoCambio).toFixed(2);
    }
}

class QRCodeAPI {
    constructor() {
        this.baseURL = 'https://quickchart.io/qr';
    }

    generarQR(texto, size = 200) {
        const params = new URLSearchParams({
            text: texto,
            size: size,
            margin: 1
        });
        return `${this.baseURL}?${params.toString()}`;
    }

    generarQRFactura(factura) {
        const texto = `Factura: ${factura.idfactura}\nFecha: ${factura.fecha}\nTotal: $${factura.total}\nCliente: ${factura.cliente}`;
        return this.generarQR(texto, 250);
    }

    generarQRConsultaFactura(idfactura) {
        const url = `${window.location.origin}/ProyectoPOO/reportes.html?factura=${idfactura}`;
        return this.generarQR(url, 200);
    }
}

const exchangeRateAPI = new ExchangeRateAPI();
const qrCodeAPI = new QRCodeAPI();

async function mostrarTipoCambio() {
    const contenedor = document.getElementById('tipoCambioWidget');
    if (!contenedor) return;

    const resultado = await exchangeRateAPI.obtenerTipoCambio();

    if (resultado.success) {
        const tipoCambio = resultado.usd_to_mxn;
        contenedor.innerHTML = `
            <div>
                <div style="text-align: center; margin-bottom: 1rem;">
                    <p style="margin: 0 0 0.5rem 0; color: #64748b; font-size: 0.8rem; font-weight: 500;">
                        1 USD equivale a
                    </p>
                    <div style="background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); padding: 1rem; border-radius: 0.75rem;">
                        <h4 style="margin: 0; font-size: 2.25rem; font-weight: 700; color: #1e40af; line-height: 1;">
                            $${tipoCambio.toFixed(2)}
                        </h4>
                        <p style="margin: 0.25rem 0 0 0; font-size: 1rem; font-weight: 600; color: #3b82f6;">
                            MXN
                        </p>
                    </div>
                </div>

                <div style="background: #f8fafc; padding: 0.875rem; border-radius: 0.5rem; margin-bottom: 0.75rem;">
                    <p style="margin: 0 0 0.625rem 0; color: #475569; font-size: 0.8rem; font-weight: 600; text-align: center;">
                        <i class="fas fa-calculator" style="color: #3b82f6;"></i> Calculadora de Conversión
                    </p>
                    <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                        <input 
                            type="number" 
                            id="cantidadUSD" 
                            placeholder="0.00" 
                            value="100"
                            min="0"
                            step="0.01"
                            style="flex: 1; padding: 0.625rem; border: 2px solid #e2e8f0; border-radius: 0.5rem; font-size: 0.875rem; text-align: center; font-weight: 600; color: #1e293b;"
                            oninput="calcularConversion(${tipoCambio})"
                        >
                        <span style="color: #64748b; font-size: 0.875rem; font-weight: 600;">USD</span>
                    </div>
                    <div style="background: white; padding: 0.75rem; border-radius: 0.5rem; border: 2px solid #10b981; text-align: center;">
                        <p style="margin: 0; font-size: 0.75rem; color: #64748b; margin-bottom: 0.25rem;">Equivale a</p>
                        <p id="resultadoMXN" style="margin: 0; font-size: 1.5rem; font-weight: 700; color: #10b981;">
                            $${(100 * tipoCambio).toFixed(2)} MXN
                        </p>
                    </div>
                </div>

                <div style="display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 0.75rem; color: #64748b;">
                    <i class="fas fa-sync-alt" style="color: #10b981;"></i>
                    <span>Actualizado ${new Date().toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })}</span>
                </div>
            </div>
        `;
        window.tipoCambioActual = resultado.usd_to_mxn;
    } else {
        contenedor.innerHTML = `
            <div style="text-align: center; padding: 1.5rem;">
                <i class="fas fa-exclamation-circle" style="font-size: 2rem; color: var(--danger-color);"></i>
                <p style="margin: 0.5rem 0 0 0; color: #dc2626; font-size: 0.9rem;">Error al cargar</p>
            </div>
        `;
    }
}

function calcularConversion(tipoCambio) {
    const cantidadUSD = parseFloat(document.getElementById('cantidadUSD').value) || 0;
    const resultadoMXN = cantidadUSD * tipoCambio;
    document.getElementById('resultadoMXN').textContent = `$${resultadoMXN.toFixed(2)} MXN`;
}

function mostrarMapa() {
    const contenedor = document.getElementById('mapaWidget');
    if (!contenedor) return;

    const ubicacion = {
        nombre: "POS Store Mérida",
        direccion: "C. 50 No. 215 x 45 y 47 Fracc, Francisco de Montejo, 97201 Mérida, Yuc.",
        lat: 21.0190,
        lng: -89.6174,
        telefono: "(999) 123-4567",
        horario: "Lun-Vie: 9:00 AM - 7:00 PM, Sáb: 9:00 AM - 2:00 PM"
    };

    contenedor.innerHTML = `
        <div style="background: #f8fafc; padding: 0.875rem; border-radius: 0.5rem; border-left: 3px solid #10b981; margin-bottom: 1rem;">
            <div style="display: flex; align-items: start; gap: 8px; margin-bottom: 8px;">
                <i class="fas fa-map-marker-alt" style="color: #ef4444; font-size: 1rem; margin-top: 2px; flex-shrink: 0;"></i> 
                <p style="margin: 0; color: #1e293b; font-size: 0.8rem; font-weight: 500; line-height: 1.4;">
                    ${ubicacion.direccion}
                </p>
            </div>
            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px; padding-left: 24px;">
                <i class="fas fa-phone" style="color: #3b82f6; font-size: 0.85rem;"></i>
                <span style="color: #475569; font-size: 0.8rem;">${ubicacion.telefono}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px; padding-left: 24px;">
                <i class="fas fa-clock" style="color: #f59e0b; font-size: 0.85rem;"></i>
                <span style="color: #475569; font-size: 0.8rem;">${ubicacion.horario}</span>
            </div>
        </div>
        
        <div style="position: relative; width: 100%; height: 200px; border-radius: 0.5rem; overflow: hidden; margin-bottom: 1rem;">
            <iframe
                width="100%"
                height="100%"
                style="border:0;"
                loading="lazy"
                allowfullscreen
                referrerpolicy="no-referrer-when-downgrade"
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3723.8!2d-89.6174!3d21.019!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x8f56771f20a1e8a9%3A0x123456789!2sFrancisco%20de%20Montejo%2C%20M%C3%A9rida%2C%20Yuc.!5e0!3m2!1ses!2smx!4v1701234567890!5m2!1ses!2smx">
            </iframe>
        </div>
        
        <div style="text-align: center;">
            <a href="https://www.google.com/maps/dir//${ubicacion.lat},${ubicacion.lng}" 
               target="_blank" 
               class="btn btn-success" 
               style="display: inline-flex; align-items: center; justify-content: center; gap: 8px; padding: 0.625rem 1.25rem; text-decoration: none; font-weight: 500; font-size: 0.875rem;">
                <i class="fas fa-directions"></i> Cómo llegar
            </a>
        </div>
    `;
}

function generarQRParaFactura(factura) {
    const qrURL = qrCodeAPI.generarQRFactura(factura);
    
    return `
        <div class="qr-factura" style="text-align: center; padding: 20px; background: white; border-radius: 0.5rem;">
            <h4 style="margin-bottom: 10px;"><i class="fas fa-qrcode"></i> Código QR de la Factura</h4>
            <img src="${qrURL}" alt="QR Factura ${factura.idfactura}" style="max-width: 250px; border: 2px solid #e2e8f0; border-radius: 0.5rem;">
            <p style="margin-top: 10px; font-size: 0.875rem; color: #64748b;">
                Escanea este código para ver los detalles de la factura #${factura.idfactura}
            </p>
            <button onclick="descargarQR('${qrURL}', 'factura-${factura.idfactura}.png')" class="btn btn-sm btn-primary" style="margin-top: 10px;">
                <i class="fas fa-download"></i> Descargar QR
            </button>
        </div>
    `;
}

async function descargarQR(url, filename) {
    try {
        const response = await fetch(url);
        const blob = await response.blob();
        const urlBlob = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = urlBlob;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(urlBlob);
        document.body.removeChild(a);
        showToast('QR descargado exitosamente', 'success');
    } catch (error) {
        showToast('Error al descargar QR', 'error');
    }
}

if (window.location.pathname.includes('index.html') || window.location.pathname.endsWith('/')) {
    window.addEventListener('load', function() {
        setTimeout(() => {
            mostrarTipoCambio();
            mostrarMapa();
        }, 500);
    });
}

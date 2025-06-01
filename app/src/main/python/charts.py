import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from io import BytesIO
import base64
import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def generate_votes_timeline(json_data):
    """Genera gráfica con colores grisáceos excepto PT en rojo"""
    try:
        votes_data = json.loads(json_data)

        if not votes_data:
            return generate_error_image("No hay datos disponibles")

        df = pd.DataFrame(votes_data)

        # Verificar columnas requeridas
        required_columns = ['time', 'PAN', 'PT', 'MOVIMIENTO', 'PRI', 'MORENA_VERDE']
        if not all(col in df.columns for col in required_columns):
            return generate_error_image("Estructura de datos incorrecta")

        # Procesamiento de fechas
        df['time'] = pd.to_datetime(df['time'], errors='coerce')
        df = df.dropna(subset=['time'])

        if df.empty:
            return generate_error_image("No hay fechas válidas")

        # Ordenar por tiempo
        df = df.sort_values('time')

        # Configuración de la gráfica
        plt.figure(figsize=(14, 8))

        # Definición de colores y estilos
        party_styles = {
            'PAN': {'color': '#6691d4', 'linestyle': '-', 'linewidth': 2.5, 'alpha': 0.7},  # Gris medio
            'PT': {'color': '#ff0000', 'linestyle': '-', 'linewidth': 3, 'alpha': 1.0},     # Rojo brillante
            'MOVIMIENTO': {'color': '#ecc88d', 'linestyle': '-', 'linewidth': 2.5, 'alpha': 0.7},  # Gris claro
            'PRI': {'color': '#d85d5d', 'linestyle': '-', 'linewidth': 2.5, 'alpha': 0.7},  # Gris oscuro
            'MORENA_VERDE': {'color': '#b5ecbc', 'linestyle': '-', 'linewidth': 2.5, 'alpha': 0.7}  # Gris más oscuro
        }

        # Graficar cada partido
        for party, style in party_styles.items():
            plt.plot(df['time'], df[party],
                    label=party,
                    **style)

        # Personalización avanzada
        plt.title('Evolución Histórica de Votos', pad=25, fontsize=16, fontweight='bold')
        plt.xlabel('Fecha y Hora', fontsize=12, labelpad=10)
        plt.ylabel('Total de Votos', fontsize=12, labelpad=10)

        # Leyenda con fondo semitransparente
        legend = plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
        legend.get_frame().set_alpha(0.8)
        legend.get_frame().set_edgecolor('#cccccc')

        # Grid y ejes
        plt.grid(True, alpha=0.2)
        plt.gca().set_axisbelow(True)  # Grid detrás de los datos

        # Rotación de fechas
        plt.xticks(rotation=45, ha='right')

        # Ajustar márgenes
        plt.tight_layout(pad=3.0)

        # Convertir a imagen
        buf = BytesIO()
        plt.savefig(buf, format='png', dpi=150, bbox_inches='tight')
        plt.close()
        return base64.b64encode(buf.getvalue()).decode('utf-8')

    except Exception as e:
        logger.error(f"Error generando gráfica: {str(e)}", exc_info=True)
        return generate_error_image(f"Error: {str(e)}")
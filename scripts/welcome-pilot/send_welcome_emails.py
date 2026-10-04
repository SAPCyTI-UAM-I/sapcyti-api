#!/usr/bin/env python3
"""
Script de envío masivo para el correo de bienvenida de la prueba piloto de SAPCyTI vía Resend.

Uso:
  # 1. Simulación sin enviar (dry-run):
  python3 send_welcome_emails.py --csv recipients.sample.csv --dry-run

  # 2. Envío real con archivo .env (creado a partir de .env.example):
  python3 send_welcome_emails.py --csv recipients.csv

  # 3. Envío con parámetros manuales:
  python3 send_welcome_emails.py \
    --csv lista_alumnos.csv \
    --api-key re_xxxxxxxxxxxx \
    --from-email "SAPCyTI <notificaciones@sapcyti.site>" \
    --reply-to "soporte.posgrado@izt.uam.mx" \
    --app-url "https://sapcyti.site" \
    --feedback-url "https://forms.gle/tuFormulario"
"""

import argparse
import csv
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


def load_env_file():
    """Carga variables desde un archivo .env si existe en el directorio local o raíz."""
    script_dir = Path(__file__).resolve().parent
    candidates = [
        script_dir / ".env",
        script_dir.parent.parent / ".env"
    ]
    for env_path in candidates:
        if env_path.exists():
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    k, v = line.split("=", 1)
                    k = k.strip()
                    v = v.strip().strip('"').strip("'")
                    if k and k not in os.environ:
                        os.environ[k] = v
            break


def render_html_template(template_str: str, params: dict) -> str:
    """
    Reemplaza expresiones de plantilla y limpia atributos Thymeleaf para
    entregar un HTML estándar y compatible con cualquier cliente de correo.
    """
    html = template_str

    app_url = params.get("appUrl", "https://sapcyti.site")
    feedback_url = params.get("feedbackUrl", "https://forms.gle/feedback-sapcyti")
    support_email = params.get("supportEmail", "soporte@sapcyti.site")
    user_name = params.get("userName", "Participante")
    username = params.get("username", "usuario@correo.uam.mx")

    # Reemplazo de variables Thymeleaf (${var})
    html = re.sub(r'\$\{appUrl[^}]*\}', app_url, html)
    html = re.sub(r'\$\{feedbackUrl[^}]*\}', feedback_url, html)
    html = re.sub(r'\$\{supportEmail[^}]*\}', support_email, html)
    html = re.sub(r'\$\{userName[^}]*\}', user_name, html)
    html = re.sub(r'\$\{username[^}]*\}', username, html)

    # Reemplazo de sintaxis mustache alternativa ({{{VAR}}} o {{VAR}})
    html = html.replace("{{{APP_URL}}}", app_url).replace("{{APP_URL}}", app_url)
    html = html.replace("{{{FEEDBACK_URL}}}", feedback_url).replace("{{FEEDBACK_URL}}", feedback_url)
    html = html.replace("{{{SUPPORT_EMAIL}}}", support_email).replace("{{SUPPORT_EMAIL}}", support_email)
    html = html.replace("{{{USER_NAME}}}", user_name).replace("{{USER_NAME}}", user_name)
    html = html.replace("{{{USERNAME}}}", username).replace("{{USERNAME}}", username)

    # Limpieza de directivas Thymeleaf (th:href, th:text, th:if, etc.)
    html = re.sub(r'\s+xmlns:th="[^"]*"', '', html)
    html = re.sub(r'\s+th:[a-zA-Z0-9_\-]+="[^"]*"', '', html)

    # Limpieza de spans vacíos
    html = re.sub(r'<span\s*>\s*</span>', '', html)

    return html


def send_email_via_resend(api_key: str, from_email: str, to_email: str, subject: str, html_content: str, reply_to: str = None) -> dict:
    """Envía un correo electrónico a través de la API REST de Resend usando urllib."""
    url = "https://api.resend.com/emails"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
        "User-Agent": "SAPCyTI-Pilot-Sender/1.0"
    }

    payload = {
        "from": from_email,
        "to": [to_email],
        "subject": subject,
        "html": html_content
    }

    if reply_to:
        payload["reply_to"] = [reply_to]

    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")

    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            return {"success": True, "data": json.loads(res_body), "status": response.status}
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8")
        return {"success": False, "error": error_msg, "status": e.code}
    except Exception as e:
        return {"success": False, "error": str(e), "status": 500}


def load_recipients_from_csv(csv_path: Path) -> list:
    """Carga los destinatarios de un archivo CSV con nombres flexibles de columnas."""
    if not csv_path.exists():
        raise FileNotFoundError(f"No se encontró el archivo CSV en: {csv_path}")

    recipients = []
    with open(csv_path, mode="r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row_idx, row in enumerate(reader, start=2):
            normalized = {k.strip().lower(): v.strip() for k, v in row.items() if k}

            nombre = normalized.get("nombre") or normalized.get("name") or normalized.get("usuario_nombre") or ""
            email = normalized.get("email") or normalized.get("correo") or normalized.get("to") or ""
            usuario = normalized.get("usuario") or normalized.get("username") or normalized.get("matricula") or email

            if not email:
                print(f"⚠️  Fila {row_idx}: Ignorada (sin correo electrónico).")
                continue

            recipients.append({
                "nombre": nombre or "Participante",
                "email": email,
                "usuario": usuario
            })

    return recipients


def main():
    load_env_file()

    parser = argparse.ArgumentParser(
        description="Envío de correos de bienvenida para la prueba piloto de SAPCyTI vía Resend."
    )
    parser.add_argument(
        "--csv",
        type=str,
        default="recipients.sample.csv",
        help="Ruta al archivo CSV con los destinatarios (columnas: nombre, email, usuario)."
    )
    parser.add_argument(
        "--api-key",
        type=str,
        default=os.getenv("RESEND_API_KEY", ""),
        help="API Key de Resend (o variable de entorno RESEND_API_KEY)."
    )
    parser.add_argument(
        "--from-email",
        type=str,
        default=os.getenv("RESEND_FROM", "SAPCyTI <soporte@sapcyti.site>"),
        help="Remitente del correo con dominio verificado (ej. 'SAPCyTI <soporte@sapcyti.site>')."
    )
    parser.add_argument(
        "--reply-to",
        type=str,
        default=os.getenv("RESEND_REPLY_TO", "soporte@sapcyti.site"),
        help="Correo opcional donde recibir respuestas de los alumnos (por defecto 'soporte@sapcyti.site')."
    )
    parser.add_argument(
        "--subject",
        type=str,
        default="SAPCyTI - Instrucciones para la prueba piloto",
        help="Asunto del correo."
    )
    parser.add_argument(
        "--app-url",
        type=str,
        default=os.getenv("APP_URL", "https://sapcyti.site"),
        help="URL pública de la aplicación web de SAPCyTI."
    )
    parser.add_argument(
        "--feedback-url",
        type=str,
        default=os.getenv("FEEDBACK_URL", "https://forms.gle/feedback-sapcyti"),
        help="URL de la encuesta de feedback para los participantes."
    )
    parser.add_argument(
        "--support-email",
        type=str,
        default=os.getenv("SUPPORT_EMAIL", "soporte@sapcyti.site"),
        help="Correo de contacto para soporte técnico visible en el cuerpo del correo."
    )
    parser.add_argument(
        "--template",
        type=str,
        default=None,
        help="Ruta opcional a la plantilla HTML (por defecto usa welcome-pilot_es.html de la API)."
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=0.6,
        help="Pausa en segundos entre envíos (por defecto 0.6s para no exceder el límite de 2 req/s de Resend)."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Modo simulación: procesa y muestra los datos sin enviar correos a la API de Resend."
    )

    args = parser.parse_args()
    script_dir = Path(__file__).resolve().parent

    # Resolver ruta de plantilla
    if args.template:
        template_path = Path(args.template)
    else:
        template_path = script_dir.parent.parent / "src" / "main" / "resources" / "templates" / "email" / "welcome-pilot_es.html"

    if not template_path.exists():
        print(f"❌ Error: No se encontró la plantilla en {template_path}")
        sys.exit(1)

    with open(template_path, "r", encoding="utf-8") as f:
        template_content = f.read()

    # Resolver CSV
    csv_path = Path(args.csv)
    if not csv_path.is_absolute():
        csv_path = script_dir / args.csv
        if not csv_path.exists():
            csv_path = Path(args.csv)

    try:
        recipients = load_recipients_from_csv(csv_path)
    except Exception as e:
        print(f"❌ Error al leer archivo CSV: {e}")
        sys.exit(1)

    total = len(recipients)
    if total == 0:
        print("⚠️  No hay destinatarios válidos para procesar en el archivo CSV.")
        sys.exit(0)

    print("=" * 65)
    print("🚀 SAPCyTI — Enviar Correos de Bienvenida (Prueba Piloto)")
    print("=" * 65)
    print(f"📁 Archivo CSV:        {csv_path.name} ({total} destinatarios)")
    print(f"📄 Plantilla:          {template_path.name}")
    print(f"✉️  Remitente:          {args.from_email}")
    if args.reply_to:
        print(f"↩️  Responder a:        {args.reply_to}")
    print(f"🌐 URL Aplicación:     {args.app_url}")
    print(f"📝 Encuesta Feedback:  {args.feedback_url}")
    print(f"🛡️  Modo Simulación:    {'SÍ (Dry-Run activo — ningún correo saldrá)' if args.dry_run else 'NO (ENVÍO REAL ACTIVO)'}")
    print("=" * 65)

    if not args.dry_run and not args.api_key:
        print("❌ Error: Se requiere una API Key de Resend (--api-key o variable RESEND_API_KEY).")
        print("💡 Tip: Para probar sin enviar correos, agrega el parámetro --dry-run")
        sys.exit(1)

    success_count = 0
    error_count = 0

    for i, r in enumerate(recipients, start=1):
        nombre = r["nombre"]
        email = r["email"]
        usuario = r["usuario"]

        params = {
            "userName": nombre,
            "username": usuario,
            "appUrl": args.app_url,
            "feedbackUrl": args.feedback_url,
            "supportEmail": args.support_email
        }

        rendered_html = render_html_template(template_content, params)

        print(f"[{i}/{total}] Destinatario: {nombre} <{email}> | Usuario: {usuario}")

        if args.dry_run:
            print("     └── 🔍 [DRY-RUN] Simulación exitosa (HTML generado correctamente).")
            success_count += 1
        else:
            result = send_email_via_resend(
                api_key=args.api_key,
                from_email=args.from_email,
                to_email=email,
                subject=args.subject,
                html_content=rendered_html,
                reply_to=args.reply_to
            )

            if result["success"]:
                email_id = result["data"].get("id", "N/A")
                print(f"     └── ✅ Enviado con éxito. Resend ID: {email_id}")
                success_count += 1
            else:
                print(f"     └── ❌ Error (HTTP {result['status']}): {result['error']}")
                error_count += 1

            if i < total and args.delay > 0:
                time.sleep(args.delay)

    print("\n" + "=" * 65)
    print("📊 Resumen de Envío:")
    print(f"   • Total procesados: {total}")
    print(f"   • Exitosos:         {success_count}")
    print(f"   • Fallidos:         {error_count}")
    print("=" * 65)


if __name__ == "__main__":
    main()

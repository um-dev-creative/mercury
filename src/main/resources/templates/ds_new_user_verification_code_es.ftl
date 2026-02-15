<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Verificación de Cuenta</title>
    <style>
        .container {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-radius: 5px;
        }
        .verification-code {
            background-color: #e9ecef;
            padding: 15px;
            margin: 20px 0;
            font-size: 24px;
            text-align: center;
            letter-spacing: 5px;
            border-radius: 5px;
        }
        .footer {
            margin-top: 30px;
            font-size: 12px;
            color: #6c757d;
            text-align: center;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <h1>¡Bienvenido a Nuestro Sitio!</h1>
    </div>

    <p>Estimado(a) ${user_name},</p>

    <p>Gracias por registrarte en nuestro sitio. Para completar tu registro, por favor utiliza el siguiente código de verificación:</p>

    <div class="verification-code">
        <strong>${vc}</strong>
    </div>

    <p>Este código expirará en 24 horas. Si no has solicitado esta verificación, por favor ignora este correo.</p>

    <div class="footer">
        <p>Este es un correo automático, por favor no respondas a este mensaje.</p>
        <p>&copy; 2025 Tu Empresa. Todos los derechos reservados.</p>
    </div>
</div>
</body>
</html>

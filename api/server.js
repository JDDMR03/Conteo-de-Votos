// Cargar las variables desde el .env
require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const mariadb = require('mariadb');
const cors = require('cors');
const bcrypt = require('bcrypt');

const app = express();
const port = process.env.PORT || 5000;

app.use(bodyParser.json());
app.use(cors());

// Conexion a MariaDB
const pool = mariadb.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
    port: parseInt(process.env.DB_PORT || 3606),
    connectionLimit: parseInt(process.env.DB_CONNECTION_LIMIT || 20)
});

app.get('/votos', async (req, res) => {
    let conn;
    try {
        conn = await pool.getConnection();
        const votes = await conn.query('SELECT * FROM votes');
        res.status(200).json({
            status: 200,
            message: "Votos obtenidos correctamente",
            data: votes
        });
    } catch (err) {
        res.status(500).json({
            status: 500,
            message: "Error al obtener los votos",
            error: err.message
        });
    } finally {
        if (conn) conn.release();
    }
});

app.post('/votos', async (req, res) => {
    const { votesPAN, votesPT, votesMOVIMIENTO, votesPRI, votesMORENAVERDE } = req.body;
    if(!votesPAN || !votesPT || !votesMOVIMIENTO || !votesPRI || !votesMORENAVERDE) {
        return res.status(400).json({
            status: 400,
            message: "Faltan datos requeridos",
        });
    }

    let conn;
    try {
        conn = await pool.getConnection();
        // Insertar el voto
        await conn.query(
            `INSERT INTO votes (votesPAN, votesPT, votesMOVIMIENTO, votesPRI, votesMORENAVERDE)
            VALUES (?, ?, ?, ?, ?)`,
            [votesPAN, votesPT, votesMOVIMIENTO, votesPRI, votesMORENAVERDE]
        );
        await conn.commit();
        res.status(201).json({
            status: 201,
            message: "Voto agregado exitosamente",
        });
    } catch (err) {
        if (conn) await conn.rollback();
        res.status(500).json({
            status: 500,
            message: "Error al agregar el voto",
            error: err.message,
        });
    } finally {
        if (conn) conn.release();
    }
});

// Obtener voto por ID
app.get('/votos/:id', async (req, res) => {
    const id = req.params.id;

    if(!id) {
        return res.status(400).json({
            status: 400,
            message: "Se requiere ID del voto",
        });
    }

    let conn;
    try {
        conn = await pool.getConnection();
        const vote = await conn.query('SELECT * FROM votes WHERE id = ?', [id]);
        
        if (vote.length === 0) {
            return res.status(404).json({
                status: 404,
                message: "Voto no encontrado",
            });
        }

        res.status(200).json({
            status: 200,
            message: "Voto obtenido correctamente",
            data: vote[0]
        });
    } catch (err) {
        res.status(500).json({
            status: 500,
            message: "Error al obtener el voto",
            error: err.message
        });
    } finally {
        if (conn) conn.release();
    }
});

// Obtener el último voto (ID más alto)
app.get('/voto/ultimo', async (req, res) => {
    let conn;
    try {
        conn = await pool.getConnection();
        const vote = await conn.query('SELECT * FROM votes ORDER BY id DESC LIMIT 1');
        
        if (vote.length === 0) {
            return res.status(404).json({
                status: 404,
                message: "No hay votos registrados",
            });
        }

        res.status(200).json({
            status: 200,
            message: "Último voto obtenido correctamente",
            data: vote[0]
        });
    } catch (err) {
        res.status(500).json({
            status: 500,
            message: "Error al obtener el último voto",
            error: err.message
        });
    } finally {
        if (conn) conn.release();
    }
});

app.listen(port, () => {
    console.log(`Servidor corriendo en http://localhost:${port}`);
});

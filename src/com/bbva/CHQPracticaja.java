package com.bbva;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;
import oracle.jdbc.driver.OracleDriver;

public class CHQPracticaja {
	private static Properties fileConfig;

	private static final Logger logger = Logger.getLogger(CHQPracticaja.class);

	public static void main(String[] args) throws SQLException {
		logger.debug("Inicia el envio de archivos con estatus ");
		conectaBD();
	}

	private static void conectaBD() throws SQLException {
		String rutaSalida = "";
		String usuario = "";
		String contrase = "";
		String rutaConfig = "";
		String url = "";
		String sHrSistema = "";
		String sFchSistema = "";
		String sHora = "";
		String sMin = "";
		String sMes = "";
		String sDia = "";
		String sQuery = "SELECT chq.CD_ORIGEN, "
				+ "chq.CD_TERMINAL, "
				+ "chq.NB_FOLIO_OPERACION, "
				+ "chq.NU_DOCUMENTO, "
				+ "chq.IM_DOCUMENTO, "
				+ "tp.NB_TP_DOCUMENTO,"
				+ "chq.NU_CTA_ABONO, "
				+ "chq.NU_CTA_CARGO, "
				+ "arc.NB_ST_ARCHIVO, "
				+ "TO_CHAR(chq.TM_ALTA,'DD/MM/YYYY HH24:MI:SS') AS TM_ALTA, "
				+ "NVL(chq.NB_CAUSA,' ') AS NB_CAUSA, "
				+ "NVL(TO_CHAR(chq.TM_MODIFICACION,'DD/MM/YYYY HH24:MI:SS'),' ') as TM_MODIFICACION "
				+ "FROM TQG038_CHQ_PRACTICAJA chq "
				+ "INNER JOIN TQG001_TP_DOCUMENT tp ON chq.CD_TP_DOCUMENTO = tp.CD_TP_DOCUMENTO "
				+ "INNER JOIN TQG020_ST_ARCHIVO arc ON chq. ST_ARCHIVO = arc.CD_ST_ARCHIVO ";
		String sConsultaAll = "WHERE chq.ST_ARCHIVO<>68 ORDER BY chq.TM_ALTA DESC";
		Boolean selectAll = Boolean.valueOf(true);
		rutaConfig = "C:\\Users\\MB78198\\invalid.properties";
		try {
			fileConfig = new Properties();
			fileConfig.load(new FileInputStream(rutaConfig));
			rutaConfig = String.valueOf(rutaSalida) + fileConfig.getProperty("rutaconfig");
			usuario = fileConfig.getProperty("dbuser");
			contrase = fileConfig.getProperty("dbpass");
			rutaSalida = String.valueOf(rutaSalida) + fileConfig.getProperty("rutasalida");
			url = fileConfig.getProperty("urlbd");
			Calendar cal = Calendar.getInstance();
			sHora = String.format("%02d", new Object[] { Integer.valueOf(cal.get(11)) });
			sMin = String.format("%02d", new Object[] { Integer.valueOf(cal.get(12)) });
			sMes = String.format("%02d", new Object[] { Integer.valueOf(cal.get(2) + 1) });
			sDia = String.format("%02d", new Object[] { Integer.valueOf(cal.get(5)) });
			sHrSistema = String.valueOf(sHora) + ":" + sMin;
			sFchSistema = "_" + sMes + sDia + sHora + sMin;
			String[] arrHorario = fileConfig.getProperty("hr.reporte").split("\\|");
			for (int i = 0; i < arrHorario.length; i++) {
				if (arrHorario[i].equals(sHrSistema)) {
					logger.info("Rango de horario correcto, se crea archivo");
					DriverManager.registerDriver((Driver) new OracleDriver());
					Connection conn = DriverManager.getConnection(url, usuario, contrase);
					selectAll = Boolean.valueOf(fileConfig.getProperty("selectAll"));
					Statement stmt = conn.createStatement();
					if (selectAll.booleanValue()) {
						sQuery = String.valueOf(sQuery) + sConsultaAll;
					} else {
						sQuery = String.valueOf(sQuery)
								+ " WHERE chq.ST_ARCHIVO<>68 and chq.TM_ALTA >= to_date(to_char(sysdate -1 , 'dd/mm/yyyy'), 'dd/mm/yyyy') ORDER BY chq.TM_ALTA DESC";
					}
					ResultSet rset = stmt.executeQuery(sQuery);
					DataOutputStream newFile = null;
					newFile = new DataOutputStream(new FileOutputStream(
							String.valueOf(rutaSalida) + fileConfig.getProperty("nameArchivo") + sFchSistema + ".txt",
							false));
					File fileIn = new File(
							String.valueOf(rutaSalida) + fileConfig.getProperty("nameArchivo") + sFchSistema + ".txt");
					FileWriter writerTxt = new FileWriter(fileIn);
					BufferedWriter bw = new BufferedWriter(writerTxt);
					PrintWriter wr = new PrintWriter(bw);
					if (rset.next()) {
						logger.debug("Existen archivos no enviados");
						do {
							wr.write(rset.getString(1));
							wr.append("|");
							wr.append(rset.getString(2));
							wr.append("|");
							wr.append(rset.getString(3));
							wr.append("|");
							wr.append(rset.getString(4));
							wr.append("|");
							wr.append(rset.getString(5));
							wr.append("|");
							wr.append(rset.getString(6));
							wr.append("|");
							wr.append(rset.getString(7));
							wr.append("|");
							wr.append(rset.getString(8));
							wr.append("|");
							wr.append(rset.getString(9));
							wr.append("|");
							wr.append(rset.getString(10));
							wr.append("|");
							wr.append(rset.getString(11));
							wr.append("|");
							wr.append(rset.getString(12));
							wr.append("\r\n");
						} while (rset.next());
					} else {
						logger.debug("No existen archivos en estatus invalidos");
					}
					logger.debug("Fin del proceso");
					wr.close();
					bw.close();
					stmt.close();
					conn.close();
				}
			}
			String sHoraDel = fileConfig.getProperty("hr.delete");
			String rutaFir = fileConfig.getProperty("ruta.firma");
			if (sHoraDel.equals(sHrSistema))
				deleteFirmas(rutaFir, "fir");
		} catch (IOException e) {
			e.printStackTrace();
			logger.debug(e);
		}
	}

	public static void deleteFirmas(String path, final String extension) {
		try {
			File[] archivos = (new File(path)).listFiles(new FileFilter() {
				public boolean accept(File archivo) {
					if (archivo.isFile())
						return archivo.getName().endsWith(String.valueOf('.') + extension);
					return false;
				}
			});
			byte b;
			int i;
			File[] arrayOfFile1;
			for (i = (arrayOfFile1 = archivos).length, b = 0; b < i;) {
				File archivo = arrayOfFile1[b];
				archivo.delete();
				logger.info("Archivo " + archivo.getName() + " borrado");
				b++;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
}

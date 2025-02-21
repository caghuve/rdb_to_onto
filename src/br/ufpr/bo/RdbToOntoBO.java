package br.ufpr.bo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import br.ufpr.bean.CheckSubject;
import br.ufpr.bean.CheckValue;
import br.ufpr.bean.Column;
import br.ufpr.bean.ColumnCheckValue;
import br.ufpr.bean.ColumnRecordValue;
import br.ufpr.bean.ColumnToDatatypeProperty;
import br.ufpr.bean.ColumnToObjectProperty;
import br.ufpr.bean.Database;
import br.ufpr.bean.DatabaseDomain;
import br.ufpr.bean.DatatypeDb;
import br.ufpr.bean.DatatypeOnto;
import br.ufpr.bean.DatatypeProperty;
import br.ufpr.bean.DatatypePropertyDomain;
import br.ufpr.bean.Disjoint;
import br.ufpr.bean.Hierarchy;
import br.ufpr.bean.Instance;
import br.ufpr.bean.ObjectProperty;
import br.ufpr.bean.ObjectPropertyDomainRange;
import br.ufpr.bean.Ontology;
import br.ufpr.bean.Record;
import br.ufpr.bean.Table;
import br.ufpr.bean.TableDatabaseDomain;
import br.ufpr.bean.Type;
import br.ufpr.dao.CheckSubjectDao;
import br.ufpr.dao.CheckValueDao;
import br.ufpr.dao.ClassDao;
import br.ufpr.dao.ColumnCheckValueDao;
import br.ufpr.dao.ColumnDao;
import br.ufpr.dao.ColumnRecordValueDao;
import br.ufpr.dao.ColumnToDatatypePropertyDao;
import br.ufpr.dao.ColumnToObjectPropertyDao;
import br.ufpr.dao.DatabaseDao;
import br.ufpr.dao.DatabaseDomainDao;
import br.ufpr.dao.DatatypeDbDao;
import br.ufpr.dao.DatatypeOntoDao;
import br.ufpr.dao.DatatypePropertyDao;
import br.ufpr.dao.DatatypePropertyDomainDao;
import br.ufpr.dao.DisjointDao;
import br.ufpr.dao.HierarchyDao;
import br.ufpr.dao.InstanceDao;
import br.ufpr.dao.ObjectPropertyDao;
import br.ufpr.dao.ObjectPropertyDomainRangeDao;
import br.ufpr.dao.OntologyDao;
import br.ufpr.dao.RecordDao;
import br.ufpr.dao.TableDao;
import br.ufpr.dao.TableDatabaseDomainDao;
import br.ufpr.dao.TypeDao;
import br.ufpr.form.RdbToOntoForm;
import br.ufpr.util.Util;

public class RdbToOntoBO {

	DatabaseDao databaseDao = new DatabaseDao();
	TableDao tableDao = new TableDao();
	ColumnDao columnDao = new ColumnDao();
	DatatypeDbDao datatypeDbDao = new DatatypeDbDao();
	DatatypeOntoDao datatypeOntoDao = new DatatypeOntoDao();
	CheckSubjectDao checkSubjectDao = new CheckSubjectDao();
	CheckValueDao checkValueDao = new CheckValueDao();
	ColumnCheckValueDao columnCheckValueDao = new ColumnCheckValueDao();
	DatabaseDomainDao databaseDomainDao = new DatabaseDomainDao();
	TableDatabaseDomainDao tableDatabaseDomainDao = new TableDatabaseDomainDao();
	OntologyDao ontologyDao = new OntologyDao();
	ClassDao classDao = new ClassDao();
	DisjointDao disjointDao = new DisjointDao();
	HierarchyDao hierarchyDao = new HierarchyDao();
	InstanceDao instanceDao = new InstanceDao();
	DatatypePropertyDao datatypePropertyDao = new DatatypePropertyDao();
	ColumnToDatatypePropertyDao columnToDatatypePropertyDao = new ColumnToDatatypePropertyDao();
	ObjectPropertyDao objectPropertyDao = new ObjectPropertyDao();
	ColumnToObjectPropertyDao columnToObjectPropertyDao = new ColumnToObjectPropertyDao();
	ObjectPropertyDomainRangeDao objectPropertyDomainRangeDao = new ObjectPropertyDomainRangeDao();
	DatatypePropertyDomainDao datatypePropertyDomainDao = new DatatypePropertyDomainDao();
	RecordDao recordDao = new RecordDao();
	ColumnRecordValueDao columnRecordValueDao = new ColumnRecordValueDao();
	TypeDao typeDao = new TypeDao();

	public Database importFile(RdbToOntoForm form) {

		long init;
		long end;
		long diff; 
		init = System.currentTimeMillis();
		System.out.println("Inicio " + init + " segundos");
		
		Database database = new Database();
		Ontology ontology = null;

		// PASSO 40
		initializeTbType();
		
		database.setName(form.getDatabaseName().toLowerCase());

		// Salvando o nome do banco de dados na T001.
		databaseDao.saveOrUpdate(database); // PASSO 2

		try {
			// Chama a função que importa a primeira ontologia na T016.
			ontology = importOntology(database); // PASSO 3.0
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			// Chama a função que importa as tabelas na T002.
			importTables(form, database); // PASSO 4
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			// Chama a função que importa as colunas na T003.
			importColumns(form, database); // PASSO 7
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		try {
			// Chama a função que importa os valores na T011.
			importClass(database, ontology); // PASSO 3.2, PASSO 3.5, PASSO 6.1?, PASSO 17.1
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			// Chama a função que importa os valores na T015.
			importInstance(); // PASSO 17.2
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}

		try {
			// Chama a função que importa os valores na T013.
			importDatatypeProperty(database, ontology); // PASSO 19, PASSO 20
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		
		try {
			// Chama a função que importa os valores nas tabelas T019, T021 e T022.
			importObjectProperty(database, ontology); // PASSO 18.1, PASSO 18.2, PASSO 22, PASSO 27, PASSO 30
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		
		try {
			// Chama a função que importa as instâncias na T004.
			importRecords(form, database, ontology); // PASSO 33
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		
		try {
			// Os registros inseridos na T004 em que o T004.c002_table_id é T002.c002_table_type = C serão transformados em classes na T011.
			importRecordsToClasses(form, database, ontology); // PASSO 34
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		
		try {
			// PASSO 40
			convertTbType(database);
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		
		/*try {
			// Classes Disjuntas
			disjointClasses(database);
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}*/
		
		end = System.currentTimeMillis();
		diff = end - init;
		System.out.println("Demorou " + (diff / 1000) + " segundos");
		
		return database;
	}

	/**
	 * 
	 * @param database
	 */
	public Ontology importOntology(Database database) {
		Ontology ontology = new Ontology();
		ontology.setName(database.getName());
		ontology.setDatabase(database);
		ontologyDao.saveOrUpdate(ontology); // PASSO 3.0
		return ontology;
	}

	/**
	 * 
	 * @param form
	 * @param database
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void importTables(RdbToOntoForm form, Database database) throws FileNotFoundException, IOException {
		Scanner scanner = new Scanner(form.getDatabaseStructure().getInputStream(), "UTF-8");

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();

			String[] fields = line.split(";", -1);

			// Inserir as tabelas.
			if (fields[0].equals("T")) {
				System.out.println(line);

				Table table = new Table();
				table.setDatabase(database);
				table.setPhysicalName(fields[1]);
				table.setLogicalName(fields[2]);
				table.setDescription(fields[3]);//table.setDescription(fields[old4]);
				table.setAssociative("1".equals(fields[5]) ? true : false);//table.setAssociative("1".equals(fields[old6]) ? true : false);

				// Cadastrando na T002.
				tableDao.saveOrUpdate(table); // PASSO 5
				
				DatabaseDomain databaseDomain = databaseDomainDao.getByDescription(database, fields[4]);//DatabaseDomain databaseDomain = databaseDomainDao.getByDescription(database, fields[old5]);
				
				// Se não encontrou o databaseDomain (T008) no banco, deve cadastrá-lo.
				if (databaseDomain == null) {
					databaseDomain = new DatabaseDomain();
					databaseDomain.setDatabase(database);
					databaseDomain.setDescription(fields[4].toLowerCase());//databaseDomain.setDescription(fields[old5].toLowerCase());
					databaseDomainDao.saveOrUpdate(databaseDomain);
				}

				TableDatabaseDomain tableDatabaseDomain = new TableDatabaseDomain();
				tableDatabaseDomain.setTable(table);
				tableDatabaseDomain.setDatabaseDomain(databaseDomain);

				// Cadastrando o relacionamento na T010.
				tableDatabaseDomainDao.saveOrUpdate(tableDatabaseDomain);			
			}
		}
		scanner.close();
	}

	/**
	 * 
	 * @param form
	 * @param database
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void importColumns(RdbToOntoForm form, Database database) throws FileNotFoundException, IOException {
		Scanner scanner = new Scanner(form.getDatabaseStructure().getInputStream(), "UTF-8");

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] fields = line.split(";", -1);

			// Inserir as colunas.
			// Se tem indAssociativeKey = 0 e IndColumnCheck = 0. // PASSO 9
			if ("C".equals(fields[0]) 
					/*&& "0".equals(fields[old14])*/ 
					&& "0".equals(fields[13])//&& "0".equals(fields[old19])
				) {
				System.out.println(line);
				importColumn(database, fields);
			}
		}

		scanner.close();

		scanner = new Scanner(form.getDatabaseStructure().getInputStream(), "UTF-8");

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] fields = line.split(";", -1);

			// Inserir as colunas.
			// Se tem IndColumnCheck = 1. // PASSO 16
			if ("C".equals(fields[0]) 
					&& "1".equals(fields[13])//&& "1".equals(fields[old19])
				) {
				System.out.println(line);
				importColumn(database, fields);
			}
		}

		scanner.close();
	}

	/**
	 * 
	 * @param database
	 * @param fields
	 */
	public void importColumn(Database database, String[] fields) {
		Column column = new Column();

		column.setPhysicalName(fields[1]);
		column.setLogicalName(fields[2]);

		// Se o logical name vier vazio, coloco o physical name em seu lugar.
		if (column.getLogicalName() == null || "".equals(column.getLogicalName())) {
			column.setLogicalName(Util.funcaoMaiuscula(column.getPhysicalName()));			
		}

		//column.setLogicalName2(fields[old3]);

		Table table = tableDao.getByPhysicalName(database.getId(), fields[6]); // PASSO 10//Table table = tableDao.getByPhysicalName(database.getId(), fields[old7]); // PASSO 10

		// table será null quando o sistema não encontrar a tabela com a qual a coluna importada é relacionada.
		// Isso pode ocorrer quando a tabela for associativa, pois no momento essas tabelas não estão sendo importadas.
		if (table == null) {
			return;
		}

		column.setTable(table);

		DatatypeDb datatypeDb = null;
		// Verifica se informou o datatype.
		// PASSO 11
		if (!"".equals(fields[7])) {
			datatypeDb = datatypeDbDao.getByDescription(fields[7]); 
		}
		else {
			datatypeDb = datatypeDbDao.getByDescription("string"); 
		}
		
		// Se encontrou o datatype no banco.
		if (datatypeDb != null) {
			column.setDatatypeDb(datatypeDb);
		}
		else {
			// Deve cadastrar o datatype (T005).
			datatypeDb = new DatatypeDb();
			if (!"".equals(fields[7])) {
				datatypeDb.setDescription(fields[7].toLowerCase());
			}
			else {
				datatypeDb.setDescription("string");
			}
			datatypeDbDao.saveOrUpdate(datatypeDb); // PASSO 11
			column.setDatatypeDb(datatypeDb);

			// Clonar o datatype na T018.
			DatatypeOnto datatypeOnto = new DatatypeOnto();
			datatypeOnto.setDescription(datatypeDb.getDescription());
			datatypeOnto.setDatatypeDb(datatypeDb);
			datatypeOntoDao.saveOrUpdate(datatypeOnto); // PASSO 11.1
		}			

		column.setPrimaryKey("1".equals(fields[8]) ? true : false);//column.setPrimaryKey("1".equals(fields[old9]) ? true : false);
		column.setUniqueKey("1".equals(fields[9]) ? true : false); // PASSO 12.1//column.setUniqueKey("1".equals(fields[old10]) ? true : false); // PASSO 12.1
		column.setForeignKey("1".equals(fields[10]) ? true : false);//column.setForeignKey("1".equals(fields[old11]) ? true : false);

		Table fkTable = tableDao.getByPhysicalName(database.getId(), fields[11]);//Table fkTable = tableDao.getByPhysicalName(database.getId(), fields[old12]);
		column.setFkTable(fkTable); // PASSO 13

		column.setIndDescription("1".equals(fields[12]) ? true : false);//column.setIndDescription("1".equals(fields[old13]) ? true : false);
		//column.setIndAssociativeKey("1".equals(fields[old14]) ? true : false);

		/*Table akTableId1 = tableDao.getByPhysicalName(database.getId(), fields[old15]);
		column.setAkTableId1(akTableId1); // PASSO 15

		if (akTableId1 != null) {
			Column akColumnId1 = columnDao.getByPhysicalName(akTableId1.getId(), fields[old16]);
			column.setAkColumnId1(akColumnId1);
		}*/

		/*Table akTableIdN = tableDao.getByPhysicalName(database.getId(), fields[old17]);
		column.setAkTableIdN(akTableIdN);

		if (akTableIdN != null) {
			Column akColumnN = columnDao.getByPhysicalName(akTableIdN.getId(), fields[old18]);
			column.setAkColumnN(akColumnN);
		}*/

		column.setIndColumnCheck("1".equals(fields[13]) ? true : false);//column.setIndColumnCheck("1".equals(fields[old19]) ? true : false);

		// Salvando a coluna (T003).
		columnDao.saveOrUpdate(column); // PASSO 12

		// Se informou o checkSubject. // PASSO 17
		if (!"".equals(fields[16])) {//if (!"".equals(fields[old22])) {
			CheckSubject checkSubject = checkSubjectDao.getByDescription(fields[16]);//CheckSubject checkSubject = checkSubjectDao.getByDescription(fields[old22]);

			// Se não encontrou o checkSubject no banco, deve cadastrá-lo. 
			if (checkSubject == null) {
				checkSubject = new CheckSubject();
				checkSubject.setDescription(fields[16].toLowerCase());//checkSubject.setDescription(fields[old22].toLowerCase());
				// Inserindo na T007.
				checkSubjectDao.saveOrUpdate(checkSubject); // PASSO 17
			}

			String[] checkValues = fields[14].split(",", -1);//String[] checkValues = fields[old20].split(",", -1);
			String[] checkAbreviations = fields[15].split(",", -1);//String[] checkAbreviations = fields[old21].split(",", -1);

			for (int j = 0; j < checkValues.length; j++) {

				// Verificar se um checkValue com essa description + abreviation já existe na T006.
				CheckValue checkValue = checkValueDao.getByDescriptionAndAbreviation(checkValues[j], checkAbreviations[j]);

				// Se não existir, deve ser cadastrado.
				if (checkValue == null) {
					checkValue = new CheckValue();
					checkValue.setDescription(checkValues[j].trim());
					checkValue.setAbreviation(checkAbreviations[j]);
					checkValue.setCheckSubject(checkSubject);
					// Inserindo na T006.
					checkValueDao.saveOrUpdate(checkValue);
				}
				
				// Inserindo na T009.
				ColumnCheckValue columnCheckValue = new ColumnCheckValue();
				columnCheckValue.setColumn(column);
				columnCheckValue.setCheckValue(checkValue);
				columnCheckValueDao.saveOrUpdate(columnCheckValue); // PASSO 17
			}
		}
	}

	/**
	 * Cadastra os valores na T011.
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importClass(Database database, Ontology ontology) {
		// Chama a função que importa a class Thing na T011.
		//br.ufpr.bean.Class clazz = importClassThing(ontology); // PASSO 3.2

		// Chama a função que importa os valores de database domain (T008) na T011.
		//importClassDatabaseDomain(database, clazz, ontology); // PASSO 3.5, PASSO 3.5a, PASSO 3.6

		// Chama a função que importa os valores de tables (T002) na T011.
		importClassTable(database, /*clazz,*/ ontology); // PASSO 6.1?, PASSO 6.1a, PASSO 6.1b, PASSO 6.2

		// Chama a função que importa os valores de check subject (T007) na T011.
		importClassCheckSubject(database, /*clazz,*/ ontology); // PASSO 17.1
	}

	/**
	 * 
	 * @param ontology
	 * @return
	 */
	public br.ufpr.bean.Class importClassThing(Ontology ontology) {
		br.ufpr.bean.Class c = new br.ufpr.bean.Class();
		c.setName("Thing");
		c.setOntology(ontology);
		classDao.saveOrUpdate(c); // PASSO 3.2
		return c;
	}

	/**
	 * Busca todos os registros da T008 relacionados a determinado banco de dados.
	 * Para cada um dos registros encontrados, insere um registro na T011.
	 * Além disso, insere um registro na T012 relacionando o registro recém-inserido na T011 e a Thing.
	 * 
	 * @param database
	 * @param clazz
	 * @param ontology
	 */
	public void importClassDatabaseDomain(Database database, br.ufpr.bean.Class clazz, Ontology ontology) {
		List<DatabaseDomain> lista = databaseDomainDao.getByDatabase(database);

		if (lista == null || lista.size() == 0) {
			return;
		}

		String name;

		for (DatabaseDomain databaseDomain : lista) {
			br.ufpr.bean.Class c = new br.ufpr.bean.Class();
			c.setDatabaseDomain(databaseDomain);
			name = Util.funcaoMaiuscula(databaseDomain.getDescription());
			name = "g" + name; // PASSO 3.5a
			c.setName(name);
			c.setOntology(ontology);
			classDao.saveOrUpdate(c); // PASSO 3.5

			Hierarchy hierarchy = new Hierarchy();
			hierarchy.setSuperClass(clazz);
			hierarchy.setSubClass(c);

			hierarchyDao.saveOrUpdate(hierarchy); // PASSO 3.6
		}
	}

	/**
	 * Busca todos os registros da T002 relacionados a determinado banco de dados.
	 * Para cada um dos registros encontrados, insere um registro na T011.
	 * Além disso, insere um registro na T012 relacionando o registro recém-inserido na T011 e a Thing.
	 * 
	 * @param database
	 * @param clazz
	 * @param ontology
	 */
	public void importClassTable(Database database, /*br.ufpr.bean.Class clazz,*/ Ontology ontology) {
		List<Table> lista = tableDao.getByDatabase(database);

		if (lista == null || lista.size() == 0) {
			return;
		}

		String name;

		for (Table table : lista) {
			
			if (table.isAssociative()) {

				Column Column = columnDao.getByTableAndPrimaryKeyAndForeignKey(table.getId(),false, false);
				if(Column == null) {
					continue;
				}
			}
			
			br.ufpr.bean.Class c = new br.ufpr.bean.Class();
			c.setTable(table);
			name = Util.funcaoMaiuscula(table.getLogicalName());

			if (name == null || "".equals(name)) {
				name = Util.funcaoMaiuscula(table.getPhysicalName()); // PASSO 6.1a
			}

			//name = "E" + name; // PASSO 6.1b
			c.setName(name);
			c.setOntology(ontology);
			classDao.saveOrUpdate(c); // PASSO 6.1

			/*Hierarchy hierarchy = new Hierarchy();
			hierarchy.setSuperClass(clazz);
			hierarchy.setSubClass(c);

			hierarchyDao.saveOrUpdate(hierarchy);*/ // PASSO 6.2
		}
	}

	/**
	 * Busca todos os registros da T007 que ainda não estão na T011.
	 * Para cada um dos registros encontrados, insere um registro na T011.
	 * Além disso, insere um registro na T012 relacionando o registro recém-inserido na T011 e a Thing.
	 * 
	 * @param database
	 * @param clazz
	 * @param ontology
	 */
	public void importClassCheckSubject(Database database, /*br.ufpr.bean.Class clazz,*/ Ontology ontology) {
		// Busca todos os itens da T007.
		List<CheckSubject> lista = checkSubjectDao.getAll();

		if (lista == null || lista.size() == 0) {
			return;
		}

		String name;
		br.ufpr.bean.Class clazzAux;

		for (CheckSubject checkSubject : lista) {
			// Busca na T011 se o item da T007 existe.
			clazzAux = classDao.getByCheckSubject(checkSubject);

			// Se não existe na T011, deve cadastrar.
			if (clazzAux == null) {
				br.ufpr.bean.Class c = new br.ufpr.bean.Class();
				c.setCheckSubject(checkSubject);
				name = Util.funcaoMaiuscula(checkSubject.getDescription());
				//name = "V" + name;
				c.setName(name);
				c.setOntology(ontology);
				classDao.saveOrUpdate(c); // PASSO 17.1

				/*Hierarchy hierarchy = new Hierarchy();
				hierarchy.setSuperClass(clazz);
				hierarchy.setSubClass(c);

				hierarchyDao.saveOrUpdate(hierarchy);*/ // PASSO 
			}
		}
	}

	/**
	 * 
	 */
	public void importInstance() {
		// Buscar todos os itens da T011 que contenham algum valor na C007.
		List<br.ufpr.bean.Class> lista = classDao.getWhenCheckSubjectNotNull();

		if (lista == null || lista.size() == 0) {
			return;
		}

		Instance instance;
		String description;

		// Para cada item encontrado na T011, verificar na T015 se ele existe (através do C011_CLASS_ID).
		for (br.ufpr.bean.Class clazz : lista) {

			instance = instanceDao.getByClass(clazz);

			// Se não existir nenhum registro na T015 relacionado ao C011 passado, deve ser inserido.
			if (instance == null) {
				// Buscar na T006 quais os valores relacionados a esse C007.
				List<CheckValue> checkValues = checkValueDao.getByCheckSubject(clazz.getCheckSubject());

				if (checkValues == null || checkValues.size() == 0) {
					continue;
				}

				// Para cada registro encontrado, inserir um valor na T015.
				for (CheckValue checkValue : checkValues) {
					Instance newIntance = new Instance();

					description = clazz.getName();
					description = Util.funcaoMinuscula(description) + "_" + Util.funcaoMaiuscula(checkValue.getDescription());
					newIntance.setDescription(description);

					newIntance.setClazz(clazz);
					newIntance.setOntology(clazz.getOntology());
					instanceDao.saveOrUpdate(newIntance); // PASSO 17.2
				}
			}
		}
	}

	/**
	 * 
	 */
	public void importDatatypeProperty(Database database, Ontology ontology) {
		// Colunas com C003_IND_DESCRIPTION = 1 AND C003_IND_COLUMN_CHECK = 0 AND C003_IND_FOREIGN_KEY = 0.
		importDatatypeProperty01(database, ontology); // PASSO 19

		// Colunas com C003_IND_DESCRIPTION = 0 AND C003_IND_COLUMN_CHECK = 0 AND C003_IND_FOREIGN_KEY = 0.
		importDatatypeProperty02(database, ontology); // PASSO 20
	}

	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importDatatypeProperty01(Database database, Ontology ontology) {
		// Buscar todas as colunas do database passado com C003_IND_DESCRIPTION = 1 AND C003_IND_COLUMN_CHECK = 0 AND C003_IND_FOREIGN_KEY = 0.
		List<Column> columns = columnDao.getByIndDescriptionAndIndColumnCheckAndIndForeignKey(database.getId(), true, false, false); 

		if (columns == null || columns.size() == 0) {
			return;
		}

		String description;

		for (Column column : columns) {
						
			// PASSO 26
			if (column.isPrimaryKey() || column.isForeignKey() || column.isUniqueKey()) {
				continue;
			}
			// PASSO 26
			if (column.getTable().isAssociative()) {
				br.ufpr.bean.Class result = classDao.getByTable(column.getTable());
				if(result == null) {
					continue;					
				}				
			}
			
			DatatypeProperty datatypeProperty = new DatatypeProperty();			
			description = Util.funcaoMinuscula(column.getLogicalName());
			datatypeProperty.setDescription(description);
			datatypeProperty.setOntology(ontology);
			datatypeProperty.setIndDescription(true); // PASSO 19.1
			datatypeProperty.setIndCommonConcept(true); // PASSO 20.1

			DatatypeOnto datatypeOnto = datatypeOntoDao.getByDatatypeDb(column.getDatatypeDb());

			if (datatypeOnto != null) {
				datatypeProperty.setDatatypeOnto(datatypeOnto);

				// Verificando se já existe uma coluna com esse nome registrado na T013.
				// Se já existir, não precisa cadastrar novamente. Apenas vincular na T020.
				DatatypeProperty datatypePropertyExistente = datatypePropertyDao.getByDatatypeProperty(datatypeProperty); 

				if (datatypePropertyExistente == null) {
					// Inserir na T013.
					datatypePropertyDao.saveOrUpdate(datatypeProperty); // PASSO 19
				}
				else {
					datatypeProperty = datatypePropertyExistente;
				}

				// Inserir na T020.
				ColumnToDatatypeProperty columnToDatatypeProperty = new ColumnToDatatypeProperty();
				columnToDatatypeProperty.setColumn(column);
				columnToDatatypeProperty.setDatatypeProperty(datatypeProperty);
				columnToDatatypePropertyDao.saveOrUpdate(columnToDatatypeProperty); // PASSO 19
				
				// Inserir na T014.
				DatatypePropertyDomain datatypePropertyDomain = new DatatypePropertyDomain();
				datatypePropertyDomain.setDatatypeProperty(datatypeProperty);
				datatypePropertyDomain.setClassDomain(classDao.getByTable(column.getTable()));
				datatypePropertyDomain.setDatatypeSubpropertyOf(true);
				datatypePropertyDomainDao.saveOrUpdate(datatypePropertyDomain); // PASSO 23, PASSO 24
			}
		}
	}

	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importDatatypeProperty02(Database database, Ontology ontology) {
		// Buscar todas as colunas do database passado com C003_IND_DESCRIPTION = 0 AND C003_IND_COLUMN_CHECK = 0 AND C003_IND_FOREIGN_KEY = 0.
		List<Column> columns = columnDao.getByIndDescriptionAndIndColumnCheckAndIndForeignKey(database.getId(), false, false, false);  

		if (columns == null || columns.size() == 0) {
			return;
		}

		String description;

		for (Column column : columns) {
			
			// PASSO 26
			if (column.isPrimaryKey() || column.isForeignKey() || column.isUniqueKey()) {
				continue;
			}
			
			// PASSO 26
			if (column.getTable().isAssociative()) {
				br.ufpr.bean.Class result = classDao.getByTable(column.getTable());
				if(result == null) {
					continue;					
				}				
			}
			
			DatatypeProperty datatypeProperty = new DatatypeProperty();			
			description = Util.funcaoMinuscula(column.getLogicalName());
			datatypeProperty.setDescription(description);
			datatypeProperty.setOntology(ontology);

			DatatypeOnto datatypeOnto = datatypeOntoDao.getByDatatypeDb(column.getDatatypeDb());

			if (datatypeOnto != null) {
				datatypeProperty.setDatatypeOnto(datatypeOnto);

				// Verificando se já existe uma coluna com esse nome registrado na T013.
				// Se já existir, não precisa cadastrar novamente. Apenas vincular na T020.
				DatatypeProperty datatypePropertyExistente = datatypePropertyDao.getByDatatypeProperty(datatypeProperty); 

				if (datatypePropertyExistente == null) {
					// Inserir na T013.
					datatypePropertyDao.saveOrUpdate(datatypeProperty);
				}
				else {
					datatypeProperty = datatypePropertyExistente;
					datatypeProperty.setIndCommonConcept(true);
					datatypePropertyDao.saveOrUpdate(datatypeProperty);
				}

				// Inserir na T020.
				ColumnToDatatypeProperty columnToDatatypeProperty = new ColumnToDatatypeProperty();
				columnToDatatypeProperty.setColumn(column);
				columnToDatatypeProperty.setDatatypeProperty(datatypeProperty);
				columnToDatatypePropertyDao.saveOrUpdate(columnToDatatypeProperty); // PASSO 20
				
				// Inserir na T014.
				DatatypePropertyDomain datatypePropertyDomain = new DatatypePropertyDomain();
				datatypePropertyDomain.setDatatypeProperty(datatypeProperty);
				datatypePropertyDomain.setClassDomain(classDao.getByTable(column.getTable()));
				datatypePropertyDomain.setDatatypeSubpropertyOf(true);
				datatypePropertyDomainDao.saveOrUpdate(datatypePropertyDomain); // PASSO 23
			}
		}
	}
	
	public void importObjectProperty(Database database, Ontology ontology) {
		//UK
		// Colunas com C003_IND_COLUMN_CHECK = 0 AND C003_IND_PRIMARY_KEY = 0 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 1
		importObjectProperty01(database, ontology); // PASSO 18.1
		//PK
		// Colunas com C003_IND_COLUMN_CHECK = 0 AND C003_IND_PRIMARY_KEY = 1 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 0
		importObjectProperty02(database, ontology); // PASSO 18.2
		//COLUMN_CHECK
		// Colunas com C003_IND_COLUMN_CHECK = 1 AND C003_IND_PRIMARY_KEY = 0 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 0.
		importObjectProperty03(database, ontology); // PASSO 22
		
		// Colunas com C003_IND_PRIMARY_KEY = 1 de tabelas associativas.
		importObjectProperty04(database, ontology); // PASSO 27
		//FK
		// Colunas com C003_IND_FOREIGN_KEY = 1.
		importObjectProperty05(database, ontology); // PASSO 30
	}
	
	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importObjectProperty01(Database database, Ontology ontology) {
		//UK
		// Colunas com C003_IND_COLUMN_CHECK = 0 AND C003_IND_PRIMARY_KEY = 0 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 1
		List<Column> columns = columnDao.getByIndsColumncheckPrimarykeyForeignkeyUniquekey(database.getId(), false, false, false, true);
		
		if (columns == null || columns.size() == 0) {
			return;
		}
		
		String description;
		
		for (Column column : columns) {
			
			// PASSO 26
			if (column.getTable().isAssociative()) {
				continue;
			}
			
			// PASSO 26
			if (column.isPrimaryKey() || column.isForeignKey()) {
				continue;
			}
			
			ObjectProperty objectProperty = new ObjectProperty();			
			description = "has" + Util.funcaoMaiuscula(column.getLogicalName());
			objectProperty.setDescription(description);
			objectProperty.setOntology(ontology);
			objectProperty.setMinCardinality(true); // PASSO 18.3
			objectProperty.setIndInverseFunctional(true); // PASSO 18.4
			
			// Inserir na T019.
			objectPropertyDao.saveOrUpdate(objectProperty);
			
			// Inserir na T021.
			ColumnToObjectProperty columnToObjectProperty = new ColumnToObjectProperty();
			columnToObjectProperty.setColumn(column);
			columnToObjectProperty.setObjectProperty(objectProperty);
			columnToObjectPropertyDao.saveOrUpdate(columnToObjectProperty);
			
			// Inserir na T022.
			ObjectPropertyDomainRange objectPropertyDomainRange = new ObjectPropertyDomainRange();
			objectPropertyDomainRange.setClassRange(classDao.getByTable(column.getTable())); // objectPropertyDomainRange.setClassDomain(classDao.getByTable(column.getTable()));
			objectPropertyDomainRange.setObjectProperty(objectProperty);
			objectPropertyDomainRangeDao.saveOrUpdate(objectPropertyDomainRange);
		}
	}
	
	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importObjectProperty02(Database database, Ontology ontology) {
		//PK
		// Colunas com C003_IND_COLUMN_CHECK = 0 AND C003_IND_PRIMARY_KEY = 1 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 0
		List<Column> columns = columnDao.getByIndsColumncheckPrimarykeyForeignkeyUniquekey(database.getId(), false, true, false, false);
		
		if (columns == null || columns.size() == 0) {
			return;
		}
		
		String description;
		
		for (Column column : columns) {
			
			// PASSO 26
			if (column.getTable().isAssociative()) {
				continue;
			}
			
			// PASSO 26
			if (column.isForeignKey() || column.isUniqueKey()) {
				continue;
			}
			
			ObjectProperty objectProperty = new ObjectProperty();			
			description = "has" + Util.funcaoMaiuscula(column.getLogicalName());
			objectProperty.setDescription(description);
			objectProperty.setOntology(ontology);
			objectProperty.setMinCardinality(true); // PASSO 18.3
			objectProperty.setIndInverseFunctional(true); // PASSO 18.4
			
			// Inserir na T019.
			objectPropertyDao.saveOrUpdate(objectProperty);
			
			// Inserir na T021.
			ColumnToObjectProperty columnToObjectProperty = new ColumnToObjectProperty();
			columnToObjectProperty.setColumn(column);
			columnToObjectProperty.setObjectProperty(objectProperty);
			columnToObjectPropertyDao.saveOrUpdate(columnToObjectProperty);
			
			// Inserir na T022.
			ObjectPropertyDomainRange objectPropertyDomainRange = new ObjectPropertyDomainRange();
			objectPropertyDomainRange.setClassRange(classDao.getByTable(column.getTable()));
			objectPropertyDomainRange.setObjectProperty(objectProperty);
			objectPropertyDomainRangeDao.saveOrUpdate(objectPropertyDomainRange);
		}
	}
	
	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importObjectProperty03(Database database, Ontology ontology) {
		//COLUMN_CHECK 
		// Colunas com C003_IND_COLUMN_CHECK = 1 AND C003_IND_PRIMARY_KEY = 0 AND C003_IND_FOREIGN_KEY = 0 AND C003_IND_UNIQUE_KEY = 0.
		List<Column> columns = columnDao.getByIndsColumncheckPrimarykeyForeignkeyUniquekey(database.getId(), true, false, false, false);
		
		if (columns == null || columns.size() == 0) {
			return;
		}
		
		String description;
		
		for (Column column : columns) {
			
			// PASSO 26
			if (column.getTable().isAssociative()) {
				continue;
			}
			
			// PASSO 26
			if (column.isPrimaryKey() || column.isForeignKey() || column.isUniqueKey()) {
				continue;
			}
			
			ObjectProperty objectProperty = new ObjectProperty();			
			description = Util.funcaoMinuscula(column.getLogicalName());
			objectProperty.setDescription(description);
			objectProperty.setOntology(ontology);
			
			// Inserir na T019.
			objectPropertyDao.saveOrUpdate(objectProperty); // PASSO 22
			
			// Inserir na T021.
			ColumnToObjectProperty columnToObjectProperty = new ColumnToObjectProperty();
			columnToObjectProperty.setColumn(column);
			columnToObjectProperty.setObjectProperty(objectProperty);
			columnToObjectPropertyDao.saveOrUpdate(columnToObjectProperty); // PASSO 22
			
			// Inserir na T022.
			ObjectPropertyDomainRange objectPropertyDomainRange = new ObjectPropertyDomainRange();
			objectPropertyDomainRange.setClassDomain(classDao.getByTable(column.getTable()));
			ColumnCheckValue columnCheckValue = columnCheckValueDao.getByColumn(column);
			objectPropertyDomainRange.setClassRange(classDao.getByCheckSubject(columnCheckValue.getCheckValue().getCheckSubject()));
			objectPropertyDomainRange.setObjectProperty(objectProperty);
			objectPropertyDomainRangeDao.saveOrUpdate(objectPropertyDomainRange); // PASSO 22
		}
	}
	
	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importObjectProperty04(Database database, Ontology ontology) {
		// Colunas com C003_IND_PRIMARY_KEY = 1.
		List<Column> columns = columnDao.getByIndPrimarykey(database.getId(), true); // PASSO 27
		
		if (columns == null || columns.size() == 0) {
			return;
		}
		
		String description;
		
		for (Column column : columns) {
			
			// PASSO 27
			if (!column.getTable().isAssociative()) {
				continue;
			}
			
			ObjectProperty objectProperty = new ObjectProperty();			
			//description = "kM" + Util.funcaoMaiuscula(column.getLogicalName());
			description = "has" + Util.funcaoMaiuscula(column.getLogicalName()); // PASSO 27
			objectProperty.setDescription(description);
			objectProperty.setOntology(ontology);
			objectProperty.setMinCardinality(true); // PASSO 27.1
			
			// Inserir na T019.
			objectPropertyDao.saveOrUpdate(objectProperty);
			
			// Inserir na T021.
			ColumnToObjectProperty columnToObjectProperty = new ColumnToObjectProperty();
			columnToObjectProperty.setColumn(column);
			columnToObjectProperty.setObjectProperty(objectProperty);
			columnToObjectPropertyDao.saveOrUpdate(columnToObjectProperty);
			
			// Inserir na T022.
			ObjectPropertyDomainRange objectPropertyDomainRange = new ObjectPropertyDomainRange();
			objectPropertyDomainRange.setClassRange(classDao.getByTable(column.getFkTable()));
			objectPropertyDomainRange.setObjectProperty(objectProperty);
			
			// PASSO 28
			Column columnAux = columnDao.getByIndPrimaryKeyTableAndId(column);
			objectPropertyDomainRange.setClassDomain(classDao.getByTable(columnAux.getFkTable()));
			objectPropertyDomainRangeDao.saveOrUpdate(objectPropertyDomainRange);
		}
	}
	
	/**
	 * 
	 * @param database
	 * @param ontology
	 */
	public void importObjectProperty05(Database database, Ontology ontology) {
		// Colunas com C003_IND_FOREIGN_KEY = 1.
		List<Column> columns = columnDao.getByIndForeignkey(database.getId(), true);
		
		if (columns == null || columns.size() == 0) {
			return;
		}
		
		String description;
		
		for (Column column : columns) {
			
			if (column.getTable().isAssociative()) {
				continue;
			}
			
			ObjectProperty objectProperty = new ObjectProperty();			
			description = "has" + Util.funcaoMaiuscula(column.getLogicalName()); // PASSO 30
			objectProperty.setDescription(description);
			objectProperty.setMinCardinality(true); // PASSO 18.3
			objectProperty.setIndInverseFunctional(true); // PASSO 18.4
			objectProperty.setOntology(ontology);
			
			// Inserir na T019.
			objectPropertyDao.saveOrUpdate(objectProperty); // PASSO 31
			
			// Inserir na T021.
			ColumnToObjectProperty columnToObjectProperty = new ColumnToObjectProperty();
			columnToObjectProperty.setColumn(column);
			columnToObjectProperty.setObjectProperty(objectProperty);
			columnToObjectPropertyDao.saveOrUpdate(columnToObjectProperty); // PASSO 31
			
			// Inserir na T022.
			ObjectPropertyDomainRange objectPropertyDomainRange = new ObjectPropertyDomainRange();
			br.ufpr.bean.Class classDomain = classDao.getByTable(column.getTable());
			objectPropertyDomainRange.setClassDomain(classDomain);
			br.ufpr.bean.Class classRange = classDao.getByTable(column.getFkTable());
			objectPropertyDomainRange.setClassRange(classRange);
			objectPropertyDomainRange.setObjectProperty(objectProperty);
			objectPropertyDomainRangeDao.saveOrUpdate(objectPropertyDomainRange); // PASSO 31
			
			// Inserir na T012.
			Hierarchy hierarchy = new Hierarchy();
			hierarchy.setSuperClass(classDomain);
			hierarchy.setSubClass(classRange);

			hierarchyDao.saveOrUpdate(hierarchy); // PASSO 32
		}
	}
	
	public void importRecords(RdbToOntoForm form, Database database, Ontology ontology) throws FileNotFoundException, IOException, SQLException {
		Scanner scanner = new Scanner(form.getDatabaseStructure().getInputStream(), "UTF-8");

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();

			String[] fields = line.split(";", -1);

			// Inserir as instancias.
			if (fields[0].equals("R")) {
				System.out.println(line);

				Record record = new Record();
				
				record.setTableColumns(fields[17]);//record.setTableColumns(fields[old23]);
				record.setColumnvalues(fields[18]);//record.setColumnvalues(fields[old24]);
				
				Table table = tableDao.getByPhysicalName(database.getId(), fields[6]);//Table table = tableDao.getByPhysicalName(database.getId(), fields[old7]);
				record.setTable(table);

				// Inserir na T004.
				recordDao.saveOrUpdate(record); // PASSO 33
				
				// PASSO 37
				if ("D".equals(table.getDescription())) {
					// Inserir na T015.
					Instance newIntance = new Instance();
					//String description = "i" + Util.functionForImportRecords(record.getColumnvalues()); // PASSO 37
					String description = Util.functionForImportRecords(record.getColumnvalues()); // PASSO 37
					newIntance.setDescription(description);
					newIntance.setClazz(classDao.getByTable(table));
					newIntance.setOntology(ontology);
					instanceDao.saveOrUpdate(newIntance); // PASSO 17.2
				}
				
				//Inserir na T023
				String[] tableColumns = fields[17].split(",", -1);
				String[] columnValues = fields[18].split(",", -1);
				for (int i=0; i<tableColumns.length; i++) {	
					if(!columnValues[i].equals("")) {
						ColumnRecordValue columnRecordValue = new ColumnRecordValue();
						columnRecordValue.setColumn(columnDao.getByPhysicalName(table.getId(),tableColumns[i].replaceAll("\\[", "").replaceAll("\\]","")));
						columnRecordValue.setRecord((Record) recordDao.find(Record.class, recordDao.getMaxId()));
						columnRecordValue.setRecordValue(columnValues[i].replaceAll("\\[", "").replaceAll("\\]",""));
						columnRecordValueDao.saveOrUpdate(columnRecordValue);
					}
				}		
			}
		}

		scanner.close();
	}
	
	/*public void importRecordsToClasses(RdbToOntoForm form, Database database, Ontology ontology) throws FileNotFoundException, IOException {
		// PASSO 34
		//Verificar quais registros da T004 são relacionados a tabelas que possuem uma coluna com C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 0.
		@SuppressWarnings("unchecked")
		List<Record> records = (ArrayList<Record>) recordDao.findAll(Record.class);
		
		Table table;
		Column column;
		
		for (Record record : records) {
			table = null;
			column = null;
			
			table = record.getTable();
			
			if ("C".equals(table.getDescription())) {
				// Verificar se a tabela possui alguma coluna C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 0.
				
				column = columnDao.getByTableAndPrimaryKeyAndForeignKey(table.getId(), true, false);
				
				// Se for null, significa que a tabela não possui nenhuma coluna com C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 0.
				// Portanto, deve ser importada no PASSO 35.
				if (column != null) {
					// Inserir na T011.
					br.ufpr.bean.Class c = new br.ufpr.bean.Class();
					String name = "h" + Util.functionForImportRecords(record.getColumnvalues()); // PASSO 35
					c.setName(name);
					c.setOntology(ontology);
					c.setRecord(record);
					classDao.saveOrUpdate(c); // PASSO 35
					
					// Inserir na T012.
					Hierarchy hierarchy = new Hierarchy();
					hierarchy.setSuperClass(classDao.getByTable(table));
					hierarchy.setSubClass(c);

					hierarchyDao.saveOrUpdate(hierarchy); // PASSO 35 
				}
			}
		}
		
		for (Record record : records) {
			table = null;
			column = null;
			
			table = record.getTable();
			
			if ("C".equals(table.getDescription())) {
				// Verificar se a tabela possui alguma coluna C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 1.
				
				column = columnDao.getByTableAndPrimaryKeyAndForeignKey(table.getId(), true, true);
				
				// Se for diferente de null, significa que a tabela não possui nenhuma coluna com C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 1.
				// Portanto, deve ser importada no PASSO 36.
				if (column != null) {
					// Inserir na T011.
					br.ufpr.bean.Class c = new br.ufpr.bean.Class();
					String name = "h" + Util.functionForImportRecords(record.getColumnvalues()); // PASSO 36
					c.setName(name);
					c.setOntology(ontology);
					c.setRecord(record);
					classDao.saveOrUpdate(c); // PASSO 36
					
					// Inserir na T012.
					Hierarchy hierarchy = new Hierarchy();
					hierarchy.setSuperClass(classDao.getByTable(table));
					hierarchy.setSubClass(c);

					hierarchyDao.saveOrUpdate(hierarchy); // PASSO 36
					
					// Inserir na T012.
					Hierarchy hierarchy2 = new Hierarchy();
					hierarchy2.setSuperClass(classDao.getByTable(column.getFkTable()));
					hierarchy2.setSubClass(c);

					hierarchyDao.saveOrUpdate(hierarchy2); // PASSO 36
				}
			}
		}
	}*/
	
	public void importRecordsToClasses(RdbToOntoForm form, Database database, Ontology ontology) throws FileNotFoundException, IOException {
		// PASSO 34
		//Verificar quais registros da T004 são relacionados a tabelas que possuem uma coluna com C003_PRIMARY_KEY = 1 AND C003_FOREIGN_KEY = 0.
		@SuppressWarnings("unchecked")
		List<Record> records = (ArrayList<Record>) recordDao.findAll(Record.class);
		
		Table table;
		Column columnFk;
		Column columnSubClass;
		ColumnRecordValue columnRecordValue;
		
		for (Record record : records) {
			table = null;
			columnFk = null;
			
			table = record.getTable();
			
			if ("C".equals(table.getDescription())) {
				
				// Inserir na T011.
				br.ufpr.bean.Class c = new br.ufpr.bean.Class();
				String name = Util.functionForImportRecords(record.getColumnvalues()); // PASSO 35
				c.setName(name);
				c.setOntology(ontology);
				c.setRecord(record);
				classDao.saveOrUpdate(c); // PASSO 35
				
				// Inserir na T012.
				//Para cada registro e gerada uma subclasse para a respectiva tabela do registro.
				Hierarchy hierarchy = new Hierarchy();
				hierarchy.setSuperClass(classDao.getByTable(table));
				hierarchy.setSubClass(c);

				hierarchyDao.saveOrUpdate(hierarchy); // PASSO 35 
				
				// Verificar se a tabela possui alguma coluna  C003_FOREIGN_KEY = 1.
				columnFk = columnDao.getByTableAndForeignKey(table.getId(), true);
				
				// Se for diferente de null, significa que a tabela  possui coluna com C003_FOREIGN_KEY = 1.
				// Portanto, deve ser importada no PASSO 36.
				if (columnFk != null) {
					
					//List Com todos os registros na T023 para essa column FK - pegar 0 C004_record, C003_column_id e C023_column value				
					//select T023.c003_column_id, T023.C004_record_id, T023.c023_column_value  from rdbtoonto.t023_column_record_value T023  where T023.C003_COLUMN_ID= 31098
					List<ColumnRecordValue> columnRecordValues = (ArrayList<ColumnRecordValue>) columnRecordValueDao.listAllByRecord(record);
										
					for (ColumnRecordValue columnRecordValue2 : columnRecordValues) {
						//get column_id na tabela de Origem, passando o Column.physical_name e o TAble_id_FK
						columnSubClass = columnDao.getByPhysicalName(columnFk.getFkTable().getId(), columnFk.getPhysicalName());
						if (columnRecordValue2.getColumn().getPhysicalName().equals(columnSubClass.getPhysicalName())) {
							//buscar pela t023, registro desta column para a T002_table_id_FK cuando C023_column_value = List.c023_column_value
							if(columnSubClass != null) {
								columnRecordValue = columnRecordValueDao.getByColumn(columnSubClass, columnRecordValue2.getRecordValue());
		
								if(columnRecordValue != null) {
									
									// Inserir na T012.
									Hierarchy hierarchy2 = new Hierarchy();
									hierarchy2.setSuperClass(classDao.getByRecord(columnRecordValue.getRecord()));
									hierarchy2.setSubClass(c);

									hierarchyDao.saveOrUpdate(hierarchy2); // PASSO 36
								}
							}	
						}			
					}			
				}					
			}
		}
	}
	
	/**
	 * Método utilizado para popular a tabela tb_type.
	 */
	public void initializeTbType() {
		// Verificando se a tabela tb_type já possui os valores.
		
		int qtd = 0;
		
		try {
			qtd = typeDao.getQtd();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Se já possui os valores na tabela, nem tenta inserir novamente.
		if (qtd > 0) {
			return;
		}
		
		// Insere os registros na tb_type.
		try {
			typeDao.insertTypes();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param database
	 */
	public void convertTbType(Database database) {
		@SuppressWarnings("unchecked")
		List<DatatypeOnto> datatypeOntos = datatypeOntoDao.findAll(DatatypeOnto.class);
		String description = "";
		Type type = null;
		
		for (DatatypeOnto datatypeOnto : datatypeOntos) {
			description = Util.cleanDataType(datatypeOnto.getDescription());
			type = typeDao.getByNameDatabase(description);
			
			if (type == null) {
				datatypeOnto.setDescription("string");
			}
			else {
				datatypeOnto.setDescription(type.getNameXml());
			}
			
			datatypeOntoDao.saveOrUpdate(datatypeOnto);
		}
	}

	/**
	 * 
	 * @param database
	 */
	public void disjointClasses(Database database) {
		List<br.ufpr.bean.Class> classList = classDao.listAll();
		//Verifica todas as classes
		for (br.ufpr.bean.Class classThis : classList) {
			for (br.ufpr.bean.Class classOther : classList) {
				//Se as classes são diferentes
				if (classThis.getId() != classOther.getId()) {
					br.ufpr.bean.Hierarchy disjointSuperClasses  = hierarchyDao.getSubClass(classThis.getId(), classOther.getId());
					br.ufpr.bean.Hierarchy disjointSubClasses = hierarchyDao.getSubClass(classOther.getId(), classThis.getId());
					//Verifica se as classes não são subclasses
					if (disjointSuperClasses == null && disjointSubClasses == null) {
						List<br.ufpr.bean.Hierarchy> subClasses = (List<Hierarchy>) hierarchyDao.listSubClasses(classThis.getId());
						//Verifica se não retornou resultado, não possui subclasse e pode criar disjoint class
						if (subClasses.size() != 0) {
							//Verifica todas as subclasses de ClassThis se classOther e´uma subclasse dela
							for (br.ufpr.bean.Hierarchy subClass : subClasses) {
								br.ufpr.bean.Hierarchy hirarchySuperClasses  = hierarchyDao.getSubClass(subClass.getSuperClass().getId(), classOther.getId());
								br.ufpr.bean.Hierarchy hirarchySubClasses = hierarchyDao.getSubClass(classOther.getId(), subClass.getSuperClass().getId());
								//Se nenhuma subclasse possui classOther como subclasse, gera registro para classe disjunta.
								if (hirarchySuperClasses == null && hirarchySubClasses == null) {
									if(disjointDao.getDisjointClass(classOther.getId(), classThis.getId()) == null){
										Disjoint disjoint = new Disjoint();
										disjoint.setClassD(classThis);
										disjoint.setDisjointClass(classOther);
										disjointDao.saveOrUpdate(disjoint); // PASSO XX
									}
								}		
							}	
						}
						else {
							if(disjointDao.getDisjointClass(classOther.getId(), classThis.getId()) == null){
								Disjoint disjoint = new Disjoint();
								disjoint.setClassD(classThis);
								disjoint.setDisjointClass(classOther);
								disjointDao.saveOrUpdate(disjoint); // PASSO XX
							}
						}
					}					
				}
			}					
		}	
	}
}
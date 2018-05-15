package ys09.data;

import ys09.model.Project;
import ys09.model.User;
import ys09.model.SignIn;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.jdbc.core.PreparedStatementSetter;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataAccess {

    private static final int MAX_TOTAL_CONNECTIONS = 16;
    private static final int MAX_IDLE_CONNECTIONS = 8;

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;


    public void setup(String driverClass, String url, String user, String pass) throws SQLException {

        //initialize the data source
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(driverClass);
        bds.setUrl(url);
        bds.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        bds.setMaxIdle(MAX_IDLE_CONNECTIONS);
        bds.setUsername(user);
        bds.setPassword(pass);
        bds.setValidationQuery("SELECT 1");
        bds.setTestOnBorrow(true);
        bds.setDefaultAutoCommit(true);

        //check that everything works OK
        bds.getConnection().close();

        //initialize the jdbc template utilitiy
        jdbcTemplate = new JdbcTemplate(bds);

        //keep the dataSource for the low-level manual example to function (not actually required)
        dataSource = bds;
    }


    public void insertProject(Project project) {
        // Insert an object to the projects array

        String query = "insert into Project (title, isDone, deadlineDate) values (?, ?, ?)";
        // Id is generated by database (Auto Increment)
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            jdbcTemplate.update(query, new Object[]{project.getTitle(), project.getIsDone(), project.getDeadlineDate()});
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }
    }


    public List<Project> getProjects() {
        //TODO: Support limits SOS
        // Creates the id
        return jdbcTemplate.query("select * from Project", new ProjectRowMapper());
    }


    public List<Project> getUserProjects(int idUser, Limits limit, Boolean isDone){
        // Return all the projects belong to user with the above id

        List<Project> projects = new ArrayList<>();
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = :id) and isDone = :done and deadlineDate <= :expDate order by deadlineDate limit :limit";
        // Use NamedParameterJdbcTemplate because of Date parsing
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", idUser);
        params.addValue("limit",  limit.getCount());
        params.addValue("expDate", limit.getStart(), Types.DATE);
        params.addValue("done", isDone);
        try {
            return namedJdbcTemplate.query(query, params, new ProjectRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return projects;
        }
    }
        /*
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection(); //borrow the connection from the pool
            ps = con.prepareStatement("select * from Project where idProject in (select Project_id from Project_has_User where User_id = ?)");
            ps.setInt(1, idUser);
            rs = ps.executeQuery();
            // After preparation, find all the entries and store them into projects list to return them
            while(rs.next()) {
                Project project = new Project(
                    rs.getInt("idProject"),
                    rs.getString("title"),
                    rs.getBoolean("isDone"),
                    rs.getDate("deadlineDate")
                );
                projects.add(project);
            }
            return projects;
        }
        catch (SQLException e) {
            //report the error as a runtime exception
            throw new RuntimeException(e.getMessage(), e);
        }
        finally {
            if (ps != null) {
                try { ps.close(); }                             //closes the ResultSet too
                catch (Exception e) { e.printStackTrace(); }    //log this (leak)
            }
            if (con != null) {
                try { con.close(); }                            //return the connection to the pool
                catch (Exception e) { e.printStackTrace(); }    //log this (leak)
            }
        }*/



    // Check if User exists into the database
    public boolean userExists(String mail) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);
        String query = "SELECT * FROM User WHERE mail = ?";

        try {
            User exist = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());   // Exists
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }        // Does not exists
    }


    // Insert User
    public int insertUser(User user) {
        // Generate Random Salt and Bcrypt
        String pw_hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(pw_hash);
        user.setIsAdmin(0);
        user.setNumOfProjects(0);
        // Insert into table with jdbc template
        // Avoid SQL injections
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement statement = con.prepareStatement("INSERT INTO User(mail, firstname, lastname, password, isAdmin, numProjects) VALUES (?, ?, ?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, user.getEmail());
                statement.setString(2, user.getFirstName());
                statement.setString(3, user.getLastName());
                statement.setString(4, user.getPassword());
                statement.setInt(5, user.getIsAdmin());
                statement.setInt(6, user.getNumOfProjects());
                return statement;
            }
        }, keyHolder);
        // Return the new generated id for user
        return keyHolder.getKey().intValue();
    }


    public int checkSignIn(SignIn signin) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);
        String query = "SELECT * FROM User WHERE mail = ?";
        String mail = signin.getEmail();
        try {
            User user = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());

            if (BCrypt.checkpw(signin.getPassword(), user.getPassword())) {
                System.out.println("It matches");
                // If it matches return JWT token !
                // Save the token to a dictionary (user,token)
                return user.getId();
            } else {
                System.out.println("It does not match");
                return 0;
            }
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    // Check User Email and Password

    public List<Project> getUserProjectsRole (int idUser, String role) {
        // Return all the projects belong to user with the above id

        List<Project> projects = new ArrayList<>();
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = ? and Project_has_User.role=?)";
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            return jdbcTemplate.query(query, new Object[]{idUser, role}, new ProjectRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return projects;
            }
    }

    public List<Integer> createAuthProjectList (int id, String role)
    {
        List<Project> projectsByRole = getUserProjectsRole(id,role);
        List<Integer> projectsByRoleID = new ArrayList<Integer>();
        for (Project project: projectsByRole)
        {
            projectsByRoleID.add(project.getId());   //make list with the ids' of projects in which
            //the current user is owner
        }
        return projectsByRoleID;
    }



}

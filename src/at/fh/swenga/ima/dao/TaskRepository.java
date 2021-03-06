package at.fh.swenga.ima.dao;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import at.fh.swenga.ima.model.StudentModel;
import at.fh.swenga.ima.model.TaskModel;

@Repository
@Transactional
public interface TaskRepository extends JpaRepository<TaskModel, Integer> {

	
	
	public TaskModel findTaskById (int id);
	public List<TaskModel> findByUserName(String userName);
	public List<TaskModel> findByTitle(String name);
	public List<TaskModel> findByDescription(String name);
	public List<TaskModel> findByStatus(Boolean TRUE);
	public List<TaskModel>findAllTasksForUser(@Param("searchString")String searchString, @Param("loggedInUser")String user);

	public List<TaskModel> findByUserNameContainsOrTitleContainsOrDescriptionContainsOrStatusContainsAllIgnoreCase(String username, String firstname, String lastname, Boolean status);
	
	@SuppressWarnings("unchecked")
	TaskModel save(TaskModel persisted);
}

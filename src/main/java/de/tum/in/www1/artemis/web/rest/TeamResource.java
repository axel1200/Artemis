package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.Team;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.TeamRepository;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.dto.TeamSearchUserDTO;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing Teams.
 */
@RestController
@RequestMapping("/api")
public class TeamResource {

    private final Logger log = LoggerFactory.getLogger(TeamResource.class);

    public static final String ENTITY_NAME = "team";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TeamRepository teamRepository;

    private final TeamService teamService;

    private final CourseService courseService;

    private final ExerciseService exerciseService;

    private final AuthorizationCheckService authCheckService;

    private final UserService userService;

    public TeamResource(TeamRepository teamRepository, TeamService teamService, CourseService courseService, ExerciseService exerciseService, UserService userService,
            AuthorizationCheckService authCheckService) {
        this.teamRepository = teamRepository;
        this.teamService = teamService;
        this.courseService = courseService;
        this.exerciseService = exerciseService;
        this.userService = userService;
        this.authCheckService = authCheckService;
    }

    /**
     * POST /exercises/{exerciseId}/teams : Create a new team for an exercise.
     *
     * @param team       the team to create
     * @param exerciseId the exercise id for which to create a team
     * @return the ResponseEntity with status 201 (Created) and with body the new team, or with status 400 (Bad Request) if the team already has an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/exercises/{exerciseId}/teams")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Team> createTeam(@RequestBody Team team, @PathVariable Long exerciseId) throws URISyntaxException {
        log.debug("REST request to save Team : {}", team);
        if (team.getId() != null) {
            throw new BadRequestAlertException("A new team cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (!team.getExercise().getId().equals(exerciseId)) {
            throw new BadRequestAlertException("The team does not belong to the specified exercise id.", ENTITY_NAME, "wrongExerciseId");
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        Exercise exercise = exerciseService.findOne(exerciseId);
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user)) {
            return forbidden();
        }
        Team result = teamService.save(exercise, team);
        return ResponseEntity.created(new URI("/api/teams/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString())).body(result);
    }

    /**
     * PUT /exercises/:exerciseId/teams : Updates an existing team.
     *
     * @param team       the team to update
     * @param exerciseId the id of the exercise that the team belongs to
     * @return the ResponseEntity with status 200 (OK) and with body the updated team, or with status 400 (Bad Request) if the team is not valid, or with status 500 (Internal
     * Server Error) if the team couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/exercises/{exerciseId}/teams")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Team> updateTeam(@RequestBody Team team, @PathVariable Long exerciseId) throws URISyntaxException {
        log.debug("REST request to update Team : {}", team);
        if (team.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!team.getExercise().getId().equals(exerciseId)) {
            throw new BadRequestAlertException("The team does not belong to the specified exercise id.", ENTITY_NAME, "wrongExerciseId");
        }
        Optional<Team> existingTeam = teamRepository.findById(team.getId());
        if (existingTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        Exercise exercise = exerciseService.findOne(exerciseId);
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user)) {
            return forbidden();
        }
        Team result = teamService.save(exercise, team);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, team.getId().toString())).body(result);
    }

    /**
     * GET /exercises/:exerciseId/teams/:id : get the "id" team.
     *
     * @param exerciseId the id of the exercise that the team belongs to
     * @param id         the id of the team to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the team, or with status 404 (Not Found)
     */
    @GetMapping("/exercises/{exerciseId}/teams/{id}")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Team> getTeam(@PathVariable Long id, @PathVariable Long exerciseId) {
        log.debug("REST request to get Team : {}", id);
        Optional<Team> optionalTeam = teamRepository.findOneWithEagerStudents(id);
        if (optionalTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Team team = optionalTeam.get();
        if (!team.getExercise().getId().equals(exerciseId)) {
            throw new BadRequestAlertException("The team does not belong to the specified exercise id.", ENTITY_NAME, "wrongExerciseId");
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        Exercise exercise = exerciseService.findOne(exerciseId);
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user) && !team.hasStudent(user)) {
            return forbidden();
        }
        return ResponseEntity.ok().body(team);
    }

    /**
     * GET /exercises/:exerciseId/teams : get all the teams of an exercise for the exercise administration page
     *
     * @param exerciseId the exerciseId of the exercise for which all teams should be returned
     * @return the ResponseEntity with status 200 (OK) and the list of teams in body
     */
    @GetMapping("/exercises/{exerciseId}/teams")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Set<Team>> getTeamsForExercise(@PathVariable Long exerciseId) {
        log.debug("REST request to get all Teams for the exercise with id : {}", exerciseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        Exercise exercise = exerciseService.findOne(exerciseId);
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user)) {
            return forbidden();
        }
        return ResponseEntity.ok().body(teamRepository.findAllByExerciseIdWithEagerStudents(exerciseId));
    }

    /**
     * DELETE /exercises/:exerciseId/teams/:id : delete the "id" team.
     *
     * @param exerciseId the id of the exercise that the team belongs to
     * @param id         the id of the team to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/exercises/{exerciseId}/teams/{id}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id, @PathVariable Long exerciseId) {
        log.debug("REST request to delete Team : {}", id);
        // TODO: Martin Wauligmann - Add audit in db and log info (see delete participation)
        User user = userService.getUserWithGroupsAndAuthorities();
        Optional<Team> optionalTeam = teamRepository.findById(id);
        if (optionalTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Team team = optionalTeam.get();
        if (!team.getExercise().getId().equals(exerciseId)) {
            throw new BadRequestAlertException("The team does not belong to the specified exercise id.", ENTITY_NAME, "wrongExerciseId");
        }
        Exercise exercise = exerciseService.findOne(team.getExercise().getId());
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise, user)) {
            return forbidden();
        }
        teamRepository.delete(team);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }

    /**
     * GET /teams?shortName={shortName} : get boolean flag whether team with shortName exists
     *
     * @param shortName the shortName of the team to check for existence
     * @return Response with status 200 (OK) and boolean flag in the body
     */
    @GetMapping("/teams") // this check is independent of the exercise (a team's shortName serves as a globally unique identifier)
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Boolean> existsTeamByShortName(@RequestParam("shortName") String shortName) {
        log.debug("REST request to check Team existence by shortName : {}", shortName);
        return ResponseEntity.ok().body(teamRepository.findOneByShortName(shortName).isPresent());
    }

    /**
     * GET /courses/:courseId/exercises/:exerciseId/team-search-users : get all users for a given course.
     *
     * @param courseId    the id of the course for which to search users
     * @param exerciseId  the id of the exercise for which to search users to join a team
     * @param loginOrName the login or name by which to search users
     * @return the ResponseEntity with status 200 (OK) and with body all users
     */
    @GetMapping("/courses/{courseId}/exercises/{exerciseId}/team-search-users")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<TeamSearchUserDTO>> searchUsersInCourse(@PathVariable Long courseId, @PathVariable Long exerciseId,
            @RequestParam("loginOrName") String loginOrName) {
        log.debug("REST request to search Users for {} in course with id : {}", loginOrName, courseId);
        // restrict result size by only allowing reasonable searches
        if (loginOrName.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query param 'loginOrName' must be three characters or longer.");
        }
        User user = userService.getUserWithGroupsAndAuthorities();
        Course course = courseService.findOne(courseId);
        if (!authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            return forbidden();
        }
        Exercise exercise = exerciseService.findOne(exerciseId);
        return ResponseEntity.ok().body(teamService.searchByLoginOrNameInCourseForExerciseTeam(course, exercise, loginOrName));
    }
}
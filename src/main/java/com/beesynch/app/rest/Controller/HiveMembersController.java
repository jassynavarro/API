package com.beesynch.app.rest.Controller;

import com.beesynch.app.rest.DTO.AddMembersRequestDTO;
import com.beesynch.app.rest.DTO.HiveMembersDTO;
import com.beesynch.app.rest.DTO.MembersTaskListDTO;
import com.beesynch.app.rest.Models.User;
import com.beesynch.app.rest.Repo.HiveMembersRepo;
import com.beesynch.app.rest.Repo.HiveRepo;
import com.beesynch.app.rest.Security.JwtAuthenticationFilter;
import com.beesynch.app.rest.Service.HiveService;
import com.beesynch.app.rest.Service.RankingService;
import com.beesynch.app.rest.Repo.UserRepo;
import com.beesynch.app.rest.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/HiveMembers")
public class HiveMembersController {
    @Autowired
    private HiveMembersRepo hiveMembersRepo;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private HiveRepo hiveRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private HiveService hiveService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @GetMapping("/")
    public List<HiveMembersDTO> getAllHiveMembers() {
        return hiveMembersRepo.getAllHiveMembers();
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<HiveMembersDTO> getHiveMemberByUsername(@PathVariable String username) {
        HiveMembersDTO memberDTO = hiveMembersRepo.findByUsername(username);
        if (memberDTO != null) {
            return ResponseEntity.ok().body(memberDTO);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/membersInfo")
    public List<MembersTaskListDTO> getMembersTaskListInfo() {
        return hiveMembersRepo.getMembersTaskListInfo();
    }

    @GetMapping("/CompletionRate/{id}")
    public Double getCompletionRate(@PathVariable Long id) {
        rankingService.updateCompletionRates();
        return hiveMembersRepo.getCompletionRate(id);
    }

     // add member to a hive db
     @PostMapping("/join")
     public ResponseEntity<String> addMemberToHive(@RequestBody AddMembersRequestDTO request) {
         System.out.println("Received request: " + request);  // Log to check if data is null
         hiveService.addMemberToHive(request.getAdminUserId(), request.getMemberUsername());
         return ResponseEntity.ok("User " + request.getMemberUsername() + " has been added to the hive and notification sent!.");
     }


    @DeleteMapping("/Remove/{userId}")
    public ResponseEntity<?> removeHiveMember(@PathVariable long userId) {
        Long loggedInUserId = userService.getLoggedInUserId();
        if (loggedInUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in.");
        }

        System.out.println("Logged-in user ID: " + loggedInUserId);

        User admin = userRepo.findById(loggedInUserId).orElse(null);
        if (admin == null || !admin.getIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the hive master can remove members.");
        }

        if (!hiveMembersRepo.existsByUserId(userId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            hiveMembersRepo.deleteHiveMember(userId);
            return ResponseEntity.ok("Deleted user with user ID: " + userId + " successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing member: " + e.getMessage());
        }
    }
}
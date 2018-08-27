package com.github.euonmyoji.newhonor.task;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.github.euonmyoji.newhonor.NewHonor;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;

import java.util.List;

import static com.github.euonmyoji.newhonor.task.DisplayHonorTaskManager.TASKS;

/**
 * @author yinyangshi
 */
public class DisplayHonorTask implements Runnable {
    private String id;
    private List<Text> values;
    private Team team;
    private int[] delays;
    private int index;
    private volatile boolean running = true;

    DisplayHonorTask(String id, List<Text> values, Team team, int[] delay) {
        if (values.size() > delay.length) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        this.values = values;
        this.team = team;
        this.delays = delay;
    }

    @Override
    public void run() {
        if (running) {
            try (Timing timing = Timings.of(NewHonor.plugin, "NewHonorDisplayTask")) {
                timing.startTimingIfSync();
                team.setPrefix(values.get(index));
                Task.builder().execute(this)
                        .delayTicks(delays[index]).name("NewHonor - displayHonor Task " + id + "#" + index)
                        .submit(NewHonor.plugin);
                index++;
                if (index == values.size()) {
                    index = 0;
                }
            } catch (IllegalArgumentException e) {
                NewHonor.logger.warn("The display value is wrong", e);
                cancel();
            } catch (Throwable ignore) {
                cancel();
            }
        }
    }

    void cancel() {
        synchronized (TASKS) {
            running = false;
            TASKS.remove(id);
        }
    }
}

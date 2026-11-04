import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { TaskPriority, TASK_PRIORITIES, UserSummary } from '../../core/models/models';
import { TaskService } from '../../core/services/task.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';

/** Create / assign (route /tasks/new) and edit (route /tasks/:id/edit) a task. */
@Component({
  selector: 'app-task-form',
  templateUrl: './task-form.component.html'
})
export class TaskFormComponent implements OnInit {
  readonly priorities = TASK_PRIORITIES;
  assignees: UserSummary[] = [];
  taskId: number | null = null;
  loading = false;
  saving = false;

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(2000)]],
    deadline: [''],
    priority: ['MEDIUM' as TaskPriority, Validators.required],
    assignedTo: [null as number | null, Validators.required]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly taskService: TaskService,
    private readonly userService: UserService,
    private readonly toast: ToastService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  get isEdit(): boolean {
    return this.taskId !== null;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.taskId = id ? Number(id) : null;

    this.userService.assignees().subscribe(list => (this.assignees = list));

    if (this.taskId !== null) {
      this.loading = true;
      this.taskService.get(this.taskId).subscribe({
        next: task => {
          this.form.patchValue({
            title: task.title,
            description: task.description ?? '',
            deadline: task.deadline ?? '',
            priority: task.priority,
            assignedTo: task.assignedToId
          });
          this.loading = false;
        },
        error: () => this.router.navigate(['/tasks'])
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request = {
      title: (v.title ?? '').trim(),
      description: v.description?.trim() ? v.description.trim() : null,
      deadline: v.deadline ? v.deadline : null,
      priority: v.priority as TaskPriority,
      assignedTo: v.assignedTo as number
    };

    this.saving = true;
    const call = this.taskId !== null
      ? this.taskService.update(this.taskId, request)
      : this.taskService.create(request);
    call.subscribe({
      next: () => {
        this.toast.success(this.isEdit ? 'Task updated.' : 'Task assigned.');
        this.router.navigate(['/tasks']);
      },
      error: () => (this.saving = false)
    });
  }

  invalid(name: 'title' | 'description' | 'assignedTo'): boolean {
    const c = this.form.get(name);
    return !!c && c.touched && c.invalid;
  }
}

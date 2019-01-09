# Labels
This document describes the default labels, which are present on all public repositories of the `prisma` organization.

We go into detail about the meaning and processess around these issues.

## prisma-label-sync
As we have 20+ labels which are being used in all public repositories of the `prisma` organization, we built a tool called `label-sync` to control these labels. The `label-sync` configuration for the Prisma organization is hosted [here](https://github.com/prisma/prisma-label-sync)

## Overview over current labels
[Here](https://github.com/prisma/prisma/labels) you can get an overview over the
current labels available for the Prisma repository.

## Category: Kind
Every issue falls into one of these kinds. Thus, every issue should have a `kind` label applied.

- `kind/bug` This applies to all issues with a `bug/x-...` label. In other words: As soon as a `bug/x-...` label has been applied, this label should also be applied at the same time. The point of this is to get an easier overview over all unexpected behaviors at the same time. The Github issue filter UI is not allowing multiple “OR” filters on issues easily, so this label helps getting an overview.
- `kind/rfc` This applies to all issues with a `rfc/...` label. The same applies as for the `kind/bug` label: It should be applied to all `rfc/...` labels. It helps the team getting an overview over all open feature requests.
- `kind/question`  This applies to all general usage questions, which are not a bug report.
- `kind/discussion` This applies to general discussions about possible future features or wishes for different behaviors without a concrete feature request.
- `kind/duplicate` This applies to duplicate issues. The process to handle duplicate issues looks like this:
	1. Find the duplicate issues
	2. Favor the issue with the best description of the problem (not necessarily the oldest one)
	3. Apply the `kind/duplicate` label to the other issues, close them and point to the single source of truth issue.
	4. Note: It’s important to declare issues as duplicates before they get closed, as otherwise people who find these issues later through the search may incorrectly assume, that the issue has been fixed, when they find it closed.

## Category: Bug
If a user reports unexpected behavior of the software, it's not necessarily a bug. The issue supervisor has to identify, if it's indeed an unintended behavior or just unintuitive behavior for the user reporting the issue.
When it’s clear that this behavior is not how the product should behave, the issue supervisor needs to decide, if the provided information is enough for a reproduction.

If that is the case, the label `bug/1-repro-available` can be applied immediately. If it’s not the case, the label `bug/0-needs-info` will be applied first and the issue supervisor needs to ask the user for the needed information in order to enable a reproduction.

As soon, as the `bug/1-repro-available` state has been reached, a support engineer can sit down and actually try to reproduce the issue.
If that reproduction was successful, the label `bug/2-confirmed` can be applied.

From this moment on, it's officially a bug. This bug can now be added to the Project `PM Core` for issues of the `prisma/prisma` repository. All other projects can follow their individual planning process. 

- `bug/0-needs-info` Problem stated, but not reproduction available
- `bug/1-repro-available` Reproduction available
- `bug/2-confirmed`The bug has been confirmed, is fully specified and can be fixed.

## Category: RFC
If a new feature or idea has been posted in an issue, it has to go through the RFC (Request for Comments) process.
The first level of the RFC process is to provide a discussion platform, in other words a straw man, which can be used by people to discuss the matter at hand.
If this description of the feature already contains enough information to start with the implementation, the label `rfx/1-draft`, which stands for “RFC Spec Draft” can be applied immediately.
Most of the time, however, the `rfc/0-needs-spec` label is more appropriate in the beginning, as often times possible solutions need to be discussed before one concrete solution has been narrowed down.
As soon as consensus has been reached about a spec draft, the `rfc/2-accepted` label can be applied. At least 2 members of the team are needed to reach consensus.
From this point on, the task of implementing this feature can be planned in the sprint process of the team.

- `rfc/0-needs-spec`
- `rfc/1-draft`
- `rfc/2-accepted`
- `rfc/x-rejected`

## Category: Area
The `area/*` labels help the team to group features and bugs by certain areas. One area could be the documentation of the project, which would be represented by `area/docs`. In the case of `prisma/prisma` another area can be `area/client`, which describes issues related to the Prisma Client.

This has two main benefits:
1. When a new feature will be planned, which impacts certain areas, all issues of these areas can be filtered and looked at from a holistic view to have the necessary context in mind when making decisions.
2. When the weekly sprint is being planned, it’s helpful to batch issues of a certain area together for increased productivity. 

- `area/docs`
- `area/...`

## Category: Status
Whereas the `kind/*` labels categorize an issue by its nature, the `status/*` labels can describe the current state of an issue.
The following states are possible:

- `status/pr-welcome` This label indicates, that the issue is very likely a fairly small task, which can be implemented without knowing the whole system.
- `status/on-hold` As soon, as an issue is blocked by an external entity, e.g. when we wait for a bug to be fixed in another library, we can apply this label. Another use-case can be, that we have a PR open, which we don’t yet want to be merged. Labels can not only be applied to issues, but also PRs.
- `status/stale` In order to keep hygiene in our issues, we are leveraging the [Stale Bot](https://github.com/probot/stale) The specific configuration can be looked up in the [.github/stale.yml](https://github.com/prisma/prisma/blob/master/.github/stale.yml) file. The configuration of time of writing this document looks like this:
	- If there hasn’t been any activity in an issue for 45 days, the stale bot applies the `status/stale` label. If there was no further activity for 10 more days, the issue will be closed. Issues with the following labels are excluded by the Stale Bot:
		  - `kind/feature`
		  - `rfc/0-needs-spec`
		  - `rfc/1-draft`
		  - `rfc/2-accepted`
		  - `bug/1-repro-available`
		  - `bug/2-confirmed`
		  - `status/on-hold`
		  - `status/candidate`
- `status/candidate` In repositories like `prisma/prisma`, hundreds of feature requests have been reported already. In order to select the ones, that fit best with the current roadmap, the `status/candidate` label will be applied. This does not at all mean, that this will be tackled soon, but rather helps the product team to plan the roadmap and select the “Candidates” that could potentially end up on the roadmap.


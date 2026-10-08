package chaos.s02.a1o1;

import chaos.s02.a1o1.meta.C2UseMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Enum-typed attributes and enum-literal conditions.
 * @version 1.0.0
 */
@RosettaDataType(value="C2Use", builder=C2Use.C2UseBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C2Use", model="chaos", builder=C2Use.C2UseBuilderImpl.class, version="1.0.0")
public interface C2Use extends RosettaModelObject {

	C2UseMeta metaData = new C2UseMeta();

	/*********************** Getter Methods  ***********************/
	C2DirEnum getDir();
	List<C2ExtEnum> getExts();
	List<? extends C2Tag> getTags();

	/*********************** Build Methods  ***********************/
	C2Use build();
	
	C2Use.C2UseBuilder toBuilder();
	
	static C2Use.C2UseBuilder builder() {
		return new C2Use.C2UseBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C2Use> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C2Use> getType() {
		return C2Use.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("dir"), C2DirEnum.class, getDir(), this);
		processor.processBasic(path.newSubPath("exts"), C2ExtEnum.class, getExts(), this);
		processRosetta(path.newSubPath("tags"), processor, C2Tag.class, getTags());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C2UseBuilder extends C2Use, RosettaModelObjectBuilder {
		C2Tag.C2TagBuilder getOrCreateTags(int index);
		@Override
		List<? extends C2Tag.C2TagBuilder> getTags();
		C2Use.C2UseBuilder setDir(C2DirEnum dir);
		C2Use.C2UseBuilder addExts(C2ExtEnum exts);
		C2Use.C2UseBuilder addExts(C2ExtEnum exts, int idx);
		C2Use.C2UseBuilder addExts(List<C2ExtEnum> exts);
		C2Use.C2UseBuilder setExts(List<C2ExtEnum> exts);
		C2Use.C2UseBuilder addTags(C2Tag tags);
		C2Use.C2UseBuilder addTags(C2Tag tags, int idx);
		C2Use.C2UseBuilder addTags(List<? extends C2Tag> tags);
		C2Use.C2UseBuilder setTags(List<? extends C2Tag> tags);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("dir"), C2DirEnum.class, getDir(), this);
			processor.processBasic(path.newSubPath("exts"), C2ExtEnum.class, getExts(), this);
			processRosetta(path.newSubPath("tags"), processor, C2Tag.C2TagBuilder.class, getTags());
		}
		

		C2Use.C2UseBuilder prune();
	}

	/*********************** Immutable Implementation of C2Use  ***********************/
	class C2UseImpl implements C2Use {
		private final C2DirEnum dir;
		private final List<C2ExtEnum> exts;
		private final List<? extends C2Tag> tags;
		
		protected C2UseImpl(C2Use.C2UseBuilder builder) {
			this.dir = builder.getDir();
			this.exts = ofNullable(builder.getExts()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.tags = ofNullable(builder.getTags()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("dir")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("dir")
		public C2DirEnum getDir() {
			return dir;
		}
		
		@Override
		@RosettaAttribute("exts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("exts")
		public List<C2ExtEnum> getExts() {
			return exts;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<? extends C2Tag> getTags() {
			return tags;
		}
		
		@Override
		public C2Use build() {
			return this;
		}
		
		@Override
		public C2Use.C2UseBuilder toBuilder() {
			C2Use.C2UseBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C2Use.C2UseBuilder builder) {
			ofNullable(getDir()).ifPresent(builder::setDir);
			ofNullable(getExts()).ifPresent(builder::setExts);
			ofNullable(getTags()).ifPresent(builder::setTags);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2Use _that = getType().cast(o);
		
			if (!Objects.equals(dir, _that.getDir())) return false;
			if (!ListEquals.listEquals(exts, _that.getExts())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (dir != null ? dir.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (exts != null ? exts.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C2Use {" +
				"dir=" + this.dir + ", " +
				"exts=" + this.exts + ", " +
				"tags=" + this.tags +
			'}';
		}
	}

	/*********************** Builder Implementation of C2Use  ***********************/
	class C2UseBuilderImpl implements C2Use.C2UseBuilder {
	
		protected C2DirEnum dir;
		protected List<C2ExtEnum> exts = new ArrayList<>();
		protected List<C2Tag.C2TagBuilder> tags = new ArrayList<>();
		
		@Override
		@RosettaAttribute("dir")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("dir")
		public C2DirEnum getDir() {
			return dir;
		}
		
		@Override
		@RosettaAttribute("exts")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("exts")
		public List<C2ExtEnum> getExts() {
			return exts;
		}
		
		@Override
		@RosettaAttribute("tags")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("tags")
		public List<? extends C2Tag.C2TagBuilder> getTags() {
			return tags;
		}
		
		@Override
		public C2Tag.C2TagBuilder getOrCreateTags(int index) {
			if (tags==null) {
				this.tags = new ArrayList<>();
			}
			return getIndex(tags, index, () -> {
						C2Tag.C2TagBuilder newTags = C2Tag.builder();
						return newTags;
					});
		}
		
		@RosettaAttribute("dir")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("dir")
		@Override
		public C2Use.C2UseBuilder setDir(C2DirEnum _dir) {
			this.dir = _dir == null ? null : _dir;
			return this;
		}
		
		@RosettaAttribute("exts")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("exts")
		@Override
		public C2Use.C2UseBuilder addExts(C2ExtEnum _exts) {
			if (_exts != null) {
				this.exts.add(_exts);
			}
			return this;
		}
		
		@Override
		public C2Use.C2UseBuilder addExts(C2ExtEnum _exts, int idx) {
			getIndex(this.exts, idx, () -> _exts);
			return this;
		}
		
		@Override
		public C2Use.C2UseBuilder addExts(List<C2ExtEnum> extss) {
			if (extss != null) {
				for (final C2ExtEnum toAdd : extss) {
					this.exts.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("exts")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("exts")
		@Override
		public C2Use.C2UseBuilder setExts(List<C2ExtEnum> extss) {
			if (extss == null) {
				this.exts = new ArrayList<>();
			} else {
				this.exts = extss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public C2Use.C2UseBuilder addTags(C2Tag _tags) {
			if (_tags != null) {
				this.tags.add(_tags.toBuilder());
			}
			return this;
		}
		
		@Override
		public C2Use.C2UseBuilder addTags(C2Tag _tags, int idx) {
			getIndex(this.tags, idx, () -> _tags.toBuilder());
			return this;
		}
		
		@Override
		public C2Use.C2UseBuilder addTags(List<? extends C2Tag> tagss) {
			if (tagss != null) {
				for (final C2Tag toAdd : tagss) {
					this.tags.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("tags")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("tags")
		@Override
		public C2Use.C2UseBuilder setTags(List<? extends C2Tag> tagss) {
			if (tagss == null) {
				this.tags = new ArrayList<>();
			} else {
				this.tags = tagss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C2Use build() {
			return new C2Use.C2UseImpl(this);
		}
		
		@Override
		public C2Use.C2UseBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2Use.C2UseBuilder prune() {
			tags = tags.stream().filter(b->b!=null).<C2Tag.C2TagBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getDir()!=null) return true;
			if (getExts()!=null && !getExts().isEmpty()) return true;
			if (getTags()!=null && getTags().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2Use.C2UseBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C2Use.C2UseBuilder o = (C2Use.C2UseBuilder) other;
			
			merger.mergeRosetta(getTags(), o.getTags(), this::getOrCreateTags);
			
			merger.mergeBasic(getDir(), o.getDir(), this::setDir);
			merger.mergeBasic(getExts(), o.getExts(), (Consumer<C2ExtEnum>) this::addExts);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2Use _that = getType().cast(o);
		
			if (!Objects.equals(dir, _that.getDir())) return false;
			if (!ListEquals.listEquals(exts, _that.getExts())) return false;
			if (!ListEquals.listEquals(tags, _that.getTags())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (dir != null ? dir.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (exts != null ? exts.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (tags != null ? tags.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C2UseBuilder {" +
				"dir=" + this.dir + ", " +
				"exts=" + this.exts + ", " +
				"tags=" + this.tags +
			'}';
		}
	}
}

package holdout.typenamedlist;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
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
import holdout.typenamedlist.meta.HolderModelFirstMeta;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * A sibling whose FIRST attribute is typed by the model List: the model List claims the simple name first (imported), java.util.List is written fully qualified after it.
 * @version 0.0.0
 */
@RosettaDataType(value="HolderModelFirst", builder=HolderModelFirst.HolderModelFirstBuilderImpl.class, version="0.0.0")
@RuneDataType(value="HolderModelFirst", model="holdout", builder=HolderModelFirst.HolderModelFirstBuilderImpl.class, version="0.0.0")
public interface HolderModelFirst extends RosettaModelObject {

	HolderModelFirstMeta metaData = new HolderModelFirstMeta();

	/*********************** Getter Methods  ***********************/
	List getSubject();
	java.util.List<String> getNames();
	java.util.List<? extends List> getOthers();

	/*********************** Build Methods  ***********************/
	HolderModelFirst build();
	
	HolderModelFirst.HolderModelFirstBuilder toBuilder();
	
	static HolderModelFirst.HolderModelFirstBuilder builder() {
		return new HolderModelFirst.HolderModelFirstBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends HolderModelFirst> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends HolderModelFirst> getType() {
		return HolderModelFirst.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("subject"), processor, List.class, getSubject());
		processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
		processRosetta(path.newSubPath("others"), processor, List.class, getOthers());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderModelFirstBuilder extends HolderModelFirst, RosettaModelObjectBuilder {
		List.ListBuilder getOrCreateSubject();
		@Override
		List.ListBuilder getSubject();
		List.ListBuilder getOrCreateOthers(int index);
		@Override
		java.util.List<? extends List.ListBuilder> getOthers();
		HolderModelFirst.HolderModelFirstBuilder setSubject(List subject);
		HolderModelFirst.HolderModelFirstBuilder addNames(String names);
		HolderModelFirst.HolderModelFirstBuilder addNames(String names, int idx);
		HolderModelFirst.HolderModelFirstBuilder addNames(java.util.List<String> names);
		HolderModelFirst.HolderModelFirstBuilder setNames(java.util.List<String> names);
		HolderModelFirst.HolderModelFirstBuilder addOthers(List others);
		HolderModelFirst.HolderModelFirstBuilder addOthers(List others, int idx);
		HolderModelFirst.HolderModelFirstBuilder addOthers(java.util.List<? extends List> others);
		HolderModelFirst.HolderModelFirstBuilder setOthers(java.util.List<? extends List> others);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("subject"), processor, List.ListBuilder.class, getSubject());
			processor.processBasic(path.newSubPath("names"), String.class, getNames(), this);
			processRosetta(path.newSubPath("others"), processor, List.ListBuilder.class, getOthers());
		}
		

		HolderModelFirst.HolderModelFirstBuilder prune();
	}

	/*********************** Immutable Implementation of HolderModelFirst  ***********************/
	class HolderModelFirstImpl implements HolderModelFirst {
		private final List subject;
		private final java.util.List<String> names;
		private final java.util.List<? extends List> others;
		
		protected HolderModelFirstImpl(HolderModelFirst.HolderModelFirstBuilder builder) {
			this.subject = ofNullable(builder.getSubject()).map(f->f.build()).orElse(null);
			this.names = ofNullable(builder.getNames()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.others = ofNullable(builder.getOthers()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("subject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subject")
		public List getSubject() {
			return subject;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public java.util.List<String> getNames() {
			return names;
		}
		
		@Override
		@RosettaAttribute("others")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("others")
		public java.util.List<? extends List> getOthers() {
			return others;
		}
		
		@Override
		public HolderModelFirst build() {
			return this;
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder toBuilder() {
			HolderModelFirst.HolderModelFirstBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(HolderModelFirst.HolderModelFirstBuilder builder) {
			ofNullable(getSubject()).ifPresent(builder::setSubject);
			ofNullable(getNames()).ifPresent(builder::setNames);
			ofNullable(getOthers()).ifPresent(builder::setOthers);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderModelFirst _that = getType().cast(o);
		
			if (!Objects.equals(subject, _that.getSubject())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			if (!ListEquals.listEquals(others, _that.getOthers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (subject != null ? subject.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			_result = 31 * _result + (others != null ? others.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderModelFirst {" +
				"subject=" + this.subject + ", " +
				"names=" + this.names + ", " +
				"others=" + this.others +
			'}';
		}
	}

	/*********************** Builder Implementation of HolderModelFirst  ***********************/
	class HolderModelFirstBuilderImpl implements HolderModelFirst.HolderModelFirstBuilder {
	
		protected List.ListBuilder subject;
		protected java.util.List<String> names = new ArrayList<>();
		protected java.util.List<List.ListBuilder> others = new ArrayList<>();
		
		@Override
		@RosettaAttribute("subject")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("subject")
		public List.ListBuilder getSubject() {
			return subject;
		}
		
		@Override
		public List.ListBuilder getOrCreateSubject() {
			List.ListBuilder result;
			if (subject!=null) {
				result = subject;
			}
			else {
				result = subject = List.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("names")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("names")
		public java.util.List<String> getNames() {
			return names;
		}
		
		@Override
		@RosettaAttribute("others")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("others")
		public java.util.List<? extends List.ListBuilder> getOthers() {
			return others;
		}
		
		@Override
		public List.ListBuilder getOrCreateOthers(int index) {
			if (others==null) {
				this.others = new ArrayList<>();
			}
			return getIndex(others, index, () -> {
						List.ListBuilder newOthers = List.builder();
						return newOthers;
					});
		}
		
		@RosettaAttribute("subject")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("subject")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder setSubject(List _subject) {
			this.subject = _subject == null ? null : _subject.toBuilder();
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("names")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addNames(String _names) {
			if (_names != null) {
				this.names.add(_names);
			}
			return this;
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addNames(String _names, int idx) {
			getIndex(this.names, idx, () -> _names);
			return this;
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addNames(java.util.List<String> namess) {
			if (namess != null) {
				for (final String toAdd : namess) {
					this.names.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("names")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("names")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder setNames(java.util.List<String> namess) {
			if (namess == null) {
				this.names = new ArrayList<>();
			} else {
				this.names = namess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("others")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("others")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addOthers(List _others) {
			if (_others != null) {
				this.others.add(_others.toBuilder());
			}
			return this;
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addOthers(List _others, int idx) {
			getIndex(this.others, idx, () -> _others.toBuilder());
			return this;
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder addOthers(java.util.List<? extends List> otherss) {
			if (otherss != null) {
				for (final List toAdd : otherss) {
					this.others.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("others")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("others")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder setOthers(java.util.List<? extends List> otherss) {
			if (otherss == null) {
				this.others = new ArrayList<>();
			} else {
				this.others = otherss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public HolderModelFirst build() {
			return new HolderModelFirst.HolderModelFirstImpl(this);
		}
		
		@Override
		public HolderModelFirst.HolderModelFirstBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder prune() {
			if (subject!=null && !subject.prune().hasData()) subject = null;
			others = others.stream().filter(b->b!=null).<List.ListBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSubject()!=null && getSubject().hasData()) return true;
			if (getNames()!=null && !getNames().isEmpty()) return true;
			if (getOthers()!=null && getOthers().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderModelFirst.HolderModelFirstBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			HolderModelFirst.HolderModelFirstBuilder o = (HolderModelFirst.HolderModelFirstBuilder) other;
			
			merger.mergeRosetta(getSubject(), o.getSubject(), this::setSubject);
			merger.mergeRosetta(getOthers(), o.getOthers(), this::getOrCreateOthers);
			
			merger.mergeBasic(getNames(), o.getNames(), (Consumer<String>) this::addNames);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderModelFirst _that = getType().cast(o);
		
			if (!Objects.equals(subject, _that.getSubject())) return false;
			if (!ListEquals.listEquals(names, _that.getNames())) return false;
			if (!ListEquals.listEquals(others, _that.getOthers())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (subject != null ? subject.hashCode() : 0);
			_result = 31 * _result + (names != null ? names.hashCode() : 0);
			_result = 31 * _result + (others != null ? others.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderModelFirstBuilder {" +
				"subject=" + this.subject + ", " +
				"names=" + this.names + ", " +
				"others=" + this.others +
			'}';
		}
	}
}
